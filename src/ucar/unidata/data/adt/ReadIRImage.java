/*
 * $Id: CacheDataSource.java,v 1.12 2007/08/17 20:34:15 jeffmc Exp $
 *
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

package ucar.unidata.data.adt;

import java.io.IOException;
import java.lang.Math;
import java.lang.String;
import java.rmi.RemoteException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import visad.FlatField;
import visad.VisADException;

public class ReadIRImage {

    private static final Logger logger = LoggerFactory.getLogger(ReadIRImage.class);

    public ReadIRImage() {
    }

    public static void ReadIRDataFile(FlatField satgrid, float cenlat, float cenlon,
                                      int SatelliteID, int satChannel, boolean isTemperature) throws IOException,
            VisADException, RemoteException {
        // Retrieve temperatures from image. This to be done in IDV
        GridUtil.Grid2D g2d = GridUtil.makeGrid2D(satgrid);
        float[][] temps = null;
        float[][][] satimage = null;
        int numx = 200;
        int numy = 200;
        float[][] LocalLatitude = new float[200][200];
        float[][] LocalLongitude = new float[200][200];
        float[][] LocalTemperature = new float[200][200];

        // now spatial subset numx by numy
        GridUtil.Grid2D g2d1 = spatialSubset(g2d, cenlat, cenlon, numx, numy);

        satimage = g2d1.getvalues();
        float[][] temp0 = satimage[0];

        if (isTemperature) {
            temps = temp0;
        } else {
            temps = im_gvtota(numx, numy, temp0, SatelliteID, satChannel);
        }

        Data.IRData_NumberRows = numy;
        Data.IRData_NumberColumns = numx;
        Data.IRData_CenterLatitude = cenlat;
        Data.IRData_CenterLongitude = cenlon;

        LocalTemperature = temps;
        LocalLatitude = g2d1.getlats();
        LocalLongitude = g2d1.getlons();

        for (int XInc = 0; XInc < numx; XInc++) {
            for (int YInc = 0; YInc < numy; YInc++) {
                // must flip x/y to y/x for ADT automated routines
                Data.IRData_Latitude[YInc][XInc] = LocalLatitude[XInc][YInc];
                Data.IRData_Longitude[YInc][XInc] = LocalLongitude[XInc][YInc];
                Data.IRData_Temperature[YInc][XInc] = LocalTemperature[XInc][YInc];
            }
        }
        int CenterXPos = Data.IRData_NumberColumns / 2;
        int CenterYPos = Data.IRData_NumberRows / 2;

        double LocalValue[] = Functions.distance_angle(
                Data.IRData_Latitude[CenterYPos][CenterXPos],
                Data.IRData_Longitude[CenterYPos][CenterXPos],
                Data.IRData_Latitude[CenterYPos + 1][CenterXPos],
                Data.IRData_Longitude[CenterYPos][CenterXPos], 1);

        Data.IRData_ImageResolution = LocalValue[0];

        History.IRCurrentRecord.date = Data.IRData_JulianDate;
        History.IRCurrentRecord.time = Data.IRData_HHMMSSTime;
        History.IRCurrentRecord.latitude = Data.IRData_CenterLatitude;
        History.IRCurrentRecord.longitude = Data.IRData_CenterLongitude;
        History.IRCurrentRecord.sattype = SatelliteID;

        int RetVal[] = Functions.adt_oceanbasin(Data.IRData_CenterLatitude,
                Data.IRData_CenterLongitude);
        Env.DomainID = RetVal[1];
        // System.out.printf("lat=%f lon=%f domainID=%d\n",Data.IRData_CenterLatitude,Data.IRData_CenterLongitude,Env.DomainID);
    }

    private static GridUtil.Grid2D spatialSubset(GridUtil.Grid2D g2d, float cenlat, float cenlon,
                                                 int numx, int numy) {
        float[][] lats = g2d.getlats();
        float[][] lons = g2d.getlons();
        float[][][] values = g2d.getvalues();
        float[][] slats = new float[numx][numy];
        float[][] slons = new float[numx][numy];
        float[][][] svalues = new float[1][numx][numy];

        int ly = lats[0].length;
        int ly0 = ly / 2;
        int lx = lats.length;
        logger.debug("lenx: {}, leny: {}", lx, ly);
        int lx0 = lx / 2;
        int ii = numx / 2, jj = numy / 2;

        for (int j = 0; j < ly - 1; j++) {
            if (Float.isNaN(lats[lx0][j])) {
                continue;
            }
            if ((lats[lx0][j] > cenlat) && (lats[lx0][j + 1] < cenlat)) {
                jj = j;
            }
        }
        for (int i = 0; i < lx - 1; i++) {
            if (Float.isNaN(lons[i][ly0])) {
                continue;
            }
            if ((lons[i][ly0] < cenlon) && (lons[i + 1][ly0] > cenlon)) {
                ii = i;
            }
        }
        int startx = ii - (numx / 2 - 1);
        int starty = jj - (numy / 2 - 1);
        logger.debug("startx: {}, starty: {}", startx, starty);
        logger.debug("numx: {}, numy: {}", numx, numy);

        if (startx < 0) {
            startx = 0;
        }
        if (starty < 0) {
            starty = 0;
        }
        for (int i = 0; i < numx; i++) {
            for (int j = 0; j < numy; j++) {
                try {
                    slats[i][j] = lats[i + startx][j + starty];
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    slats[i][j] = Float.NaN;
                }
                try {
                    slons[i][j] = lons[i + startx][j + starty];
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    slats[i][j] = Float.NaN;
                }
                try {
                    svalues[0][i][j] = values[0][i + startx][j + starty];
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    slats[i][j] = Float.NaN;
                }
            }
        }
        return new GridUtil.Grid2D(slats, slons, svalues);
    }

    /**
     * im_gvtota
     *
     * This subroutine converts GVAR counts to actual temperatures based on the
     * current image set in IM_SIMG.
     *
     * im_gvtota ( int *nvals, unsigned int *gv, float *ta, int *iret )
     *
     * Input parameters: *nvals int Number of values to convert *gv int Array of
     * GVAR count values
     *
     * Output parameters: *ta float Array of actual temperatures *iret int
     * Return value = -1 - could not open table = -2 - could not find match
     *
     *
     * Log: D.W.Plummer/NCEP 02/03 D.W.Plummer/NCEP 06/03 Add coeff G for 2nd
     * order poly conv T. Piper/SAIC 07/06 Added tmpdbl to eliminate warning
     */
    private static float[][] im_gvtota(int nx, int ny, float[][] gv, int imsorc, int imtype) {
        double c1 = 1.191066E-5;
        double c2 = 1.438833;

        int ii;
        int ip;
        int chan;
        int found;
        double Rad;
        double Teff;
        double tmpdbl;
        float[][] ta = new float[nx][ny];
        String fp = "/ucar/unidata/data/storm/ImgCoeffs.tbl";

        for (ii = 0; ii < nx; ii++) {
            for (int jj = 0; jj < ny; jj++) {
                ta[ii][jj] = Float.NaN;
            }
        }

        // Read in coefficient table if necessary.
        String s = null;
        try {
            s = IOUtil.readContents(fp);
        } catch (Exception re) {
            logger.warn("Failed to read coefficient table", re);
        }

        int i = 0;
        ImgCoeffs[] ImageConvInfo = new ImgCoeffs[50];
        for (String line : StringUtil.split(s, "\n", true, true)) {
            if (line.startsWith("!")) {
                continue;
            }
            List<String> stoks = StringUtil.split(line, " ", true, true);
            ImageConvInfo[i] = new ImgCoeffs(stoks);
            i++;
        }
        int nImgRecs = i;
        found = 0;
        ii = 0;
        while ((ii < nImgRecs) && (found == 0)) {
            int tmp = ImageConvInfo[ii].chan - 1;
            tmpdbl = (double) (tmp * tmp);
            chan = G_NINT(tmpdbl);
            if ((imsorc == ImageConvInfo[ii].sat_num) && (imtype == chan)) {
                found = 1;
            } else {
                ii++;
            }
        }

        if (found == 0) {
            return null;
        } else {
            ip = ii;
            for (ii = 0; ii < nx; ii++) {
                for (int jj = 0; jj < ny; jj++) {
                    // Convert GVAR count (gv) to Scene Radiance
                    Rad = ((double) gv[ii][jj] - ImageConvInfo[ip].scal_b) /
                            // -------------------------------------
                            ImageConvInfo[ip].scal_m;

                    Rad = Math.max(Rad, 0.0);

                    // Convert Scene Radiance to Effective Temperature
                    Teff = (c2 * ImageConvInfo[ip].conv_n) /
                            /*
                             * -------------------------------------------------- -----
                             */
                            (Math.log(1.0 + (c1 * Math.pow(ImageConvInfo[ip].conv_n, 3.0)) / Rad));
                    // Convert Effective Temperature to Temperature
                    ta[ii][jj] = (float) (ImageConvInfo[ip].conv_a + ImageConvInfo[ip].conv_b
                            * Teff + ImageConvInfo[ip].conv_g * Teff * Teff);
                }
            }
        }
        return ta;
    }

    public static int G_NINT(double x) {
        return (((x) < 0.0F) ? ((((x) - (float) ((int) (x))) <= -.5f) ? (int) ((x) - .5f)
                : (int) (x)) : ((((x) - (float) ((int) (x))) >= .5f) ? (int) ((x) + .5f)
                : (int) (x)));
    }

    public static class ImgCoeffs {
        String sat_id;
        int sat_num;
        int chan;
        int det;
        float scal_m;
        float scal_b;
        float side;
        float conv_n;
        float conv_a;
        float conv_b;
        float conv_g;

        public ImgCoeffs(List<String> toks) {
            sat_id = toks.get(0);
            sat_num = Integer.parseInt(toks.get(1));
            chan = Integer.parseInt(toks.get(2));
            det = Integer.parseInt(toks.get(3));
            scal_m = Float.parseFloat(toks.get(4));
            scal_b = Float.parseFloat(toks.get(5));
            side = Float.parseFloat(toks.get(6));
            conv_n = Float.parseFloat(toks.get(7));
            conv_a = Float.parseFloat(toks.get(8));
            conv_b = Float.parseFloat(toks.get(9));
            conv_g = Float.parseFloat(toks.get(10));
        }

        public String getSat_id() {
            return sat_id;
        }
    }
}