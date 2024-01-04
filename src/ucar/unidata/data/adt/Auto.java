/*
 * $Id: CacheDataSource.java,v 1.12 2007/08/17 20:34:15 jeffmc Exp $
 *
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static java.lang.Math.pow;

import java.io.IOException;

import javax.swing.JOptionPane;

class Remap {
    /** Block size (bytes) input file */
    int in_bfw;

    /** Block size (bytes) output file */
    int out_bfw;

    /** Number of splines/line */
    int nspl;

    /** Number of splines/elem */
    int nspe;

    /** Source blocksize */
    int slb;

    /** Dest blocksize */
    int dlb;

    /** Number of corners in line */
    int ncl;

    /** Number of corners in elem */
    int nce;
}

/** TIFF header */
class TiffHeader {
    /** Byte order */
    int order;

    /** Version */
    int version;

    /** Pointer */
    int point;
}

/**
 * TIFF directory record.
 */
class TiffRecord {
    /** TIFF tag */
    int tag;

    /** Data type */
    int type;

    /** Length */
    int length;

    /** Pointer or value */
    int voff;
}

class DisVars {
    /** Output number of lines */
    double xrectl;

    /** Output number of elems */
    double xrecte;
}

class TiffVars {
    int nbits;
    int photo;
    int unit;
    int in_lines;
    int in_elems;
    int out_lines;
    int out_elems;
}

public class Auto {

    private static int[][] MoatMaskFlagField = new int[200][200];

    private static int[][] BlackWhiteFieldArray = new int[200][200];

    private static double[][] IRData_Remap_Latitude = new double[200][200];
    private static double[][] IRData_Remap_Longitude = new double[200][200];

    private static double[][] IRData_Remap_Temperature = new double[200][200];

    private static int IRData_Remap_NumberRows;

    private static int IRData_Remap_NumberColumns;

    private static double[][] NSTempGradientArray = new double[200][200];

    private static double[][] EWTempGradientArray = new double[200][200];

    private static double[][] SpiralCenterAnalysisField = new double[50000][3];

    private static double[][] RingScoreAnalysisField = new double[750000][3];

    private static int[][] CircleFilterRowArray = new int[100][2000];

    private static int[][] CircleFilterColumnArray = new int[100][2000];

    /* I don't know this size for sure */
    /** Array containing line coordinates */
    private static double[] LineCoordinateArray = new double[5000];

    /* I don't know this size for sure */
    /** Array containing element coordinates */
    private static double[] ElementCoordinateArray = new double[5000];

    private static Remap remap_vars = new Remap();

    private static TiffVars tiff_vars = new TiffVars();

    private static double RING_WIDTH = 4.0;

    /** Minimum block size (bytes) for domap */
    private static int MINBFW = 500000;

    /** Minimum block size (lines) */
    private static int MINBLKSIZ = 10;

    public Auto() {
        IRData_Remap_NumberRows = 0;
        IRData_Remap_NumberColumns = 0;
    }

    /**
     * Additional automatic positioning of storm center location using official
     * forecasts from NHC or JTWC as input.
     *
     * Storm location will be estimated using spiral fitting and ring fitting
     * routines derived by Tony Wimmers in his MatLab routines (which have been
     * converted).
     *
     * The final position will be determined utilizing empirically defined
     * confidence factors for each method. The final storm position will be
     * returned along with a position determination flag.
     *
     * @param InputLatitudePosition
     *            Storm center latitude.
     * @param InputLongitudePosition
     *            Storm center longitude.
     *
     *            Outputs : Latitude_Return - final storm center latitude
     *            position Longitude_Return - final storm center longitude
     *            position PositioningMethodID_Return - method used to derive
     *            storm location 0-error 1-interpolation of operational forecast
     *            2-Laplacian analysis (not used anymore) 3-Warm Spot location
     *            4-10^ log spiral analysis 5-Combo method of spiral and ring
     *            analyses 6-linear extrapolation from prior locations Return :
     *            Error flag = 0
     */
    public static double[] AutoMode2(double InputLatitudePosition, double InputLongitudePosition)
            throws IOException {
        int XInc;
        int YInc;
        double SpiralCenterLatitude = -99.9;
        double SpiralCenterLongitude = -99.9;
        double SpiralCenterScore = -1.0;
        double RingFitLatitude = -99.9;
        double RingFitLongitude = -99.9;
        double RingFitScore = -1.0;
        double FinalAutoLatitude = -99.9;
        double FinalAutoLongitude = -99.9;
        double FinalAutoScore = -99.9;
        int FinalAutoFixMethod = 1;
        boolean DatelineCrossTF = false;
        boolean DoRemappingTF = true;

        /*
         * NEED TO MULTIPLY LONGITUDES BY -1.0 SINCE ADT ROUTINES WERE DEVELOPED
         * ON McIDAS-X, which uses West/East Longitudes of +1.0/-1.0. McV is
         * reversed.
         */
        InputLongitudePosition = -1.0 * InputLongitudePosition; // flip for ADT
        // routines
        for (YInc = 0; YInc < Data.IRData_NumberRows; YInc++) {
            for (XInc = 0; XInc < Data.IRData_NumberColumns; XInc++) {
                Data.IRData_Longitude[YInc][XInc] = (float) -1.0
                        * Data.IRData_Longitude[YInc][XInc];
            }
        }

        if (DoRemappingTF) {
            System.out.printf("REMAPPING DATA\n");
            IRData_Remap_NumberRows = Data.IRData_NumberRows;
            IRData_Remap_NumberColumns = Data.IRData_NumberColumns;

            double LongitudeIncrement = Data.IRData_Longitude[0][0] - Data.IRData_Longitude[0][1];
            double LatitudeIncrement = Data.IRData_Latitude[0][0] - Data.IRData_Latitude[1][1];

            if (abs(LongitudeIncrement - LatitudeIncrement) < 0.001) {
                System.out.printf("already remapped\n");
                /* data is already remapped */
                /* crosses dateline check */
                int XSizeMax = Data.IRData_NumberColumns - 1;
                int YSizeMax = Data.IRData_NumberRows - 1;
                double NWCornerLongitude = Data.IRData_Longitude[0][0];
                double NECornerLongitude = Data.IRData_Longitude[0][XSizeMax];
                double SWCornerLongitude = Data.IRData_Longitude[YSizeMax][0];
                double SECornerLongitude = Data.IRData_Longitude[YSizeMax][XSizeMax];
                /*
                 * if((NWCornerLongitude<NECornerLongitude)||(SWCornerLongitude<
                 * SECornerLongitude)) { DatelineCrossTF = true; }
                 */
                if ((NWCornerLongitude > NECornerLongitude)
                        || (SWCornerLongitude > SECornerLongitude)) {
                    DatelineCrossTF = true;
                    NWCornerLongitude = NWCornerLongitude + 360.0;

                }
                for (YInc = 0; YInc < Data.IRData_NumberRows; YInc++) {
                    for (XInc = 0; XInc < Data.IRData_NumberColumns; XInc++) {
                        IRData_Remap_Longitude[YInc][XInc] = Data.IRData_Longitude[YInc][XInc];
                        /* check for dateline crossing */
                        if (DatelineCrossTF && (IRData_Remap_Longitude[YInc][XInc] < 0.0)) {
                            IRData_Remap_Latitude[YInc][XInc] = Data.IRData_Latitude[YInc][XInc] + 360.0;
                        }
                        IRData_Remap_Latitude[YInc][XInc] = Data.IRData_Latitude[YInc][XInc];
                        IRData_Remap_Temperature[YInc][XInc] = Data.IRData_Temperature[YInc][XInc];
                    }
                }
                IRData_Remap_NumberColumns = Data.IRData_NumberColumns;
                IRData_Remap_NumberRows = Data.IRData_NumberRows;
            } else {
                /* remap data to rectilinear projection */
                RemapData();
                System.out.printf("COMPLETED REMAPPING DATA\n");
            }

            /* perform spiral analysis to determine storm center location */
            double SpiralReturn[] = SpiralCenterLowRes(InputLatitudePosition,
                    InputLongitudePosition);
            SpiralCenterLatitude = SpiralReturn[0];
            SpiralCenterLongitude = SpiralReturn[1];
            SpiralCenterScore = SpiralReturn[2];

            /*
             * System.out.printf("Spiral Score : lat=%f  lon=%f  score=%f\n",
             * SpiralCenterLatitude, SpiralCenterLongitude,SpiralCenterScore);
             */

            /*
             * redefine first guess for ring analysis as input storm location
             * point
             */
            double RingFitFirstGuessLatitude = SpiralCenterLatitude;
            double RingFitFirstGuessLongitude = SpiralCenterLongitude;

            /* calculate Moat Mask for false eye check */
            double MoatMaskTempThreshold = 237.0;
            double MoatMaskMaxRadiusDegree = 0.50;
            MoatMaskCalc(MoatMaskTempThreshold, MoatMaskMaxRadiusDegree, 1);

            /* perform ring analysis to determine storm center location */
            double RingReturn[] = RingFit(RingFitFirstGuessLatitude, RingFitFirstGuessLongitude);
            RingFitLatitude = RingReturn[0];
            RingFitLongitude = RingReturn[1];
            RingFitScore = RingReturn[2];

            /*
             * System.out.printf("Ring Score   : lat=%f  lon=%f  score=%f \n",
             * RingFitLatitude,RingFitLongitude,RingFitScore);
             */
            /*
             * System.out.printf(
             * "fglat=%f fglon=%f  SpiralCenterLatitude=%f SpiralCenterLongitude=%f rglat=%f rglon=%f\n"
             * , InputLatitudePosition,InputLongitudePosition,
             * SpiralCenterLatitude,SpiralCenterLongitude,
             * RingFitLatitude,RingFitLongitude);
             */

            /* caluculate confidence factor for combined spiral/ring analyses */
            double ScoresReturn[] = CalcScores(InputLatitudePosition, InputLongitudePosition,
                    SpiralCenterLatitude, SpiralCenterLongitude, SpiralCenterScore,
                    RingFitLatitude, RingFitLongitude, RingFitScore);
            FinalAutoLatitude = ScoresReturn[0];
            FinalAutoLongitude = ScoresReturn[1];
            FinalAutoScore = ScoresReturn[2];
            FinalAutoFixMethod = (int) ScoresReturn[3];
            /*
             * System.out.printf("SPIRAL CENTER : lat=%f  lon=%f  SCORE=%f  \n",
             * SpiralCenterLatitude,SpiralCenterLongitude,SpiralCenterScore);
             * System.out.printf("RING   CENTER : lat=%f  lon=%f  SCORE=%f  \n",
             * RingFitLatitude,RingFitLongitude,RingFitScore);
             * System.out.printf("FINAL  CENTER : lat=%f  lon=%f  SCORE=%f  \n",
             * FinalAutoLatitude,FinalAutoLongitude,FinalAutoScore);
             */
        } else {
            SpiralCenterLatitude = -99.9;
            SpiralCenterLongitude = -99.9;
            SpiralCenterScore = -1.0;
            RingFitLatitude = -99.9;
            RingFitLatitude = -99.9;
            RingFitScore = -1.0;
            FinalAutoLatitude = -99.9;
            FinalAutoLongitude = -99.9;
            FinalAutoScore = -99.9;
            FinalAutoFixMethod = 1;
        }

        int PositioningMethodID = History.IRCurrentRecord.autopos;

        double LocationReturn[] = PickFinalLocation(PositioningMethodID, InputLatitudePosition,
                InputLongitudePosition, FinalAutoLatitude, FinalAutoLongitude, FinalAutoScore,
                FinalAutoFixMethod);

        double FinalLatitude = LocationReturn[0];
        double FinalLongitude = LocationReturn[1];
        PositioningMethodID = (int) LocationReturn[3];

        if (FinalLongitude > 180.0) {
            FinalLongitude = FinalLongitude - 360.0;
        }

        /*
         * System.out.printf("TAC CENTER    :  lat=%f  lon=%f \n",
         * InputLatitudePosition,InputLongitudePosition);
         */
        /*
         * System.out.printf(
         * "AUTO CENTER   :  lat=%f  lon=%f  SCORE=%f  METHOD=%d\n",
         * FinalAutoLatitude,FinalAutoLongitude,
         * FinalAutoScore,FinalAutoFixMethod);
         */
        /*
         * System.out.printf(
         * "FINAL LOCATION:  lat=%f  lon=%f  SCORE=%f  METHOD=%d\n",
         * FinalLatitude,FinalLongitude, FinalScoreValue,PositioningMethodID);
         */

        /*
         * need to flip final Longitude value back to McV format by multiplying
         * by -1.0
         */
        FinalLongitude = -1.0 * FinalLongitude;

        return new double[] { FinalLatitude, FinalLongitude, (double) PositioningMethodID };
    }

    private static double[] SpiralCenterLowRes(double InputLatitude, double InputLongitude) {

        double[][] IRData_NormCoord_Latitude = new double[200][200];
        double[][] IRData_NormCoord_Longitude = new double[200][200];
        double[][] IRData_NormCoord_Temperature = new double[200][200];

        int XInc, YInc;
        int IRData_NormCoord_NumberRows;
        int IRData_NormCoord_NumberColumns;
        int NumPoints;
        int RingWidthLocal = (int) RING_WIDTH;

        double XOff, YOff;
        double SearchRadius = 0.0;
        double CrossScoreSum = 0.0;
        double ProxyValueX = 0.0;
        double ProxyValueY = 0.0;
        double DenominatorValue = 0.0;
        double SpiralValueX = 0.0;
        double SpiralValueY = 0.0;
        double RawCrossScore = 0.0;
        double CrossScoreClean = 0.0;
        double SpiralMeanCrossScore = 0.0;
        double SpiralMeanCrossMaxScore = 0.0;
        double SpiralMeanCrossMaxYLocation = 0.0;
        double SpiralMeanCrossMaxXLocation = 0.0;

        double SpiralCenterLatitude = -99.99;
        double SpiralCenterLongitude = -999.99;
        double SpiralCenterScore = 0.0;

        double Alpha = 5.0 * PI / 180.0;
        double OutsideFactor = 0.62;

        double OuterSearchRadiusDegree = 1.75;
        double CourseGridSpacingDegree = 0.2;
        double FineGridSpacingDegree = 0.1;
        double FilterDiscSize = pow(OuterSearchRadiusDegree + (2.0 * FineGridSpacingDegree), 2);
        double AlphaPOWP1 = 1.0 + pow(Alpha, 2);

        int ImageResolution = (int) Data.GetCurrentImageResolution();
        int IncAddVal = (ImageResolution > RingWidthLocal) ? 1
                : (RingWidthLocal - ImageResolution + 1);

        double SignFactor = abs(InputLatitude) / InputLatitude;

        YInc = (IRData_Remap_NumberRows) - 1;
        if ((InputLongitude < 0.0)
                && ((IRData_Remap_Longitude[0][0] > 180.0) || (IRData_Remap_Longitude[YInc][0] > 180.0))) {
            /*
             * System.out.printf("DATELINE CROSS... changing InputLongitude from %f"
             * ,InputLongitude);
             */
            InputLongitude = InputLongitude + 360.0;
        }

        for (YInc = 0; YInc < IRData_Remap_NumberRows; YInc++) {
            for (XInc = 0; XInc < IRData_Remap_NumberColumns; XInc++) {
                /* compute normalized coordinate system arrays */
                IRData_NormCoord_Longitude[YInc][XInc] = (IRData_Remap_Longitude[YInc][XInc] - InputLongitude)
                        * (cos(PI * InputLatitude / 180.0));
                IRData_NormCoord_Latitude[YInc][XInc] = IRData_Remap_Latitude[YInc][XInc]
                        - InputLatitude;
                IRData_NormCoord_Temperature[YInc][XInc] = IRData_Remap_Temperature[YInc][XInc];
                /*
                 * System.out.printf(
                 * "YInc=%d XInc=%d  lat=%f lon=%f  lat=%f lon=%f  temp=%f\n",
                 * YInc,XInc, IRData_Remap_Latitude[YInc][XInc],
                 * IRData_Remap_Longitude[YInc][XInc],
                 * IRData_NormCoord_Latitude[YInc][XInc],
                 * IRData_NormCoord_Longitude[YInc][XInc],
                 * IRData_Remap_Temperature[YInc][XInc]);
                 */
            }
        }
        IRData_NormCoord_NumberColumns = IRData_Remap_NumberColumns;
        IRData_NormCoord_NumberRows = IRData_Remap_NumberRows;

        /* determine lat/lon grid increment */
        /* W to E gradient */
        double LongitudeIncrement = abs(IRData_NormCoord_Longitude[0][0]
                - IRData_NormCoord_Longitude[0][1]);
        /* N to S gradient */
        double LatitudeIncrement = abs(IRData_NormCoord_Latitude[0][0]
                - IRData_NormCoord_Latitude[1][0]);
        /*
         * This is to determine longitude multiplier factor... original routines
         * were developed using negative, but McIDAS is positive in WH. So if
         * LongitudeMultFactor is negative, we know (assume) we are using
         * non-McIDAS input imagery/values, otherwise make LongitudeMultFactor
         * positive. This all assumes that image is loaded from NW to SE
         */
        double LongitudeMultFactor = IRData_NormCoord_Longitude[0][0]
                - IRData_NormCoord_Longitude[0][1];
        LongitudeMultFactor = (LongitudeMultFactor < 0.0) ? 1.0 : -1.0;
        /*
         * System.out.printf(
         * "LatitudeIncrement=%f  LongitudeIncrement=%f LongitudeMultFactor=%f\n"
         * , LatitudeIncrement, LongitudeIncrement,LongitudeMultFactor);
         */

        /* calculate gradient field */
        Gradient(IRData_NormCoord_Temperature, IRData_NormCoord_NumberColumns,
                IRData_NormCoord_NumberRows, LongitudeIncrement, LatitudeIncrement);

        for (YInc = 0; YInc < IRData_NormCoord_NumberRows - 1; YInc++) {
            for (XInc = 0; XInc < IRData_NormCoord_NumberColumns - 1; XInc++) {
                /*
                 * System.out.printf(
                 * "YInc=%d XInc=%d  remap:lat=%f lon=%f  normcoord:lat=%f lon=%f NSTempGradientArray=%f EWTempGrad=%f\n"
                 * , YInc,XInc,
                 * IRData_Remap_Latitude[YInc][XInc],IRData_Remap_Longitude
                 * [YInc][XInc],
                 * IRData_NormCoord_Latitude[YInc][XInc],IRData_NormCoord_Longitude
                 * [YInc][XInc],
                 * NSTempGradientArray[YInc][XInc],EWTempGradientArray
                 * [YInc][XInc]);
                 */

                double GradientOriginalMag = sqrt(pow(NSTempGradientArray[YInc][XInc], 2)
                        + pow(EWTempGradientArray[YInc][XInc], 2));
                double GradientLOGMag = log(1.0 + GradientOriginalMag);
                double GradientLOGReduction = 0.0;
                if (GradientLOGMag != 0.0) {
                    GradientLOGReduction = GradientLOGMag / GradientOriginalMag;
                }
                NSTempGradientArray[YInc][XInc] = GradientLOGReduction
                        * NSTempGradientArray[YInc][XInc];
                EWTempGradientArray[YInc][XInc] = GradientLOGReduction
                        * EWTempGradientArray[YInc][XInc];
            }
        }

        /* COURSE GRID */
        /* calculate cross product score at each grid point */
        /* XInc/YInc are "starting point" coordinates */
        SpiralMeanCrossMaxScore = -99.0;
        double SearchRadiusMaximum = pow(OuterSearchRadiusDegree
                + ((2.0 * CourseGridSpacingDegree) / 3.0), 2);
        for (XOff = -OuterSearchRadiusDegree; XOff <= OuterSearchRadiusDegree; XOff = XOff
                + CourseGridSpacingDegree) {
            for (YOff = -OuterSearchRadiusDegree; YOff <= OuterSearchRadiusDegree; YOff = YOff
                    + CourseGridSpacingDegree) {
                /* XOff/YOff are offset coordinates from "starting point" */
                SearchRadius = pow(XOff, 2) + pow(YOff, 2);
                if (SearchRadius <= SearchRadiusMaximum) {
                    CrossScoreSum = 0.0;
                    NumPoints = 0;
                    for (YInc = 1; YInc < IRData_NormCoord_NumberRows - 2; YInc = YInc + IncAddVal) {
                        for (XInc = 1; XInc < IRData_NormCoord_NumberColumns - 2; XInc = XInc
                                + IncAddVal) {
                            SearchRadius = pow(IRData_NormCoord_Longitude[YInc][XInc], 2)
                                    + pow(IRData_NormCoord_Latitude[YInc][XInc], 2);
                            if (SearchRadius < FilterDiscSize) {
                                ProxyValueX = LongitudeMultFactor
                                        * IRData_NormCoord_Longitude[YInc][XInc] - XOff;
                                ProxyValueY = IRData_NormCoord_Latitude[YInc][XInc] - YOff;
                                DenominatorValue = sqrt((AlphaPOWP1)
                                        * (pow(ProxyValueX, 2) + pow(ProxyValueY, 2)));
                                SpiralValueX = ((Alpha * ProxyValueX) + (SignFactor * ProxyValueY))
                                        / DenominatorValue;
                                SpiralValueY = ((Alpha * ProxyValueY) - (SignFactor * ProxyValueX))
                                        / DenominatorValue;
                                RawCrossScore = (SpiralValueX * NSTempGradientArray[YInc][XInc])
                                        - (SpiralValueY * EWTempGradientArray[YInc][XInc]);
                                CrossScoreClean = max(0.0, -RawCrossScore)
                                        + (OutsideFactor * max(0.0, RawCrossScore));
                                CrossScoreSum = CrossScoreSum + CrossScoreClean;
                                /*
                                 * System.out.printf(
                                 * "%d %d : lat=%f lon=%f  xoff=%f yoff=%f ",
                                 * "ProxyValueX=%f ProxyValueY=%f ",
                                 * "SpiralValueX=%f SpiralValueY=%f ",
                                 * "rawScore=%f clean=%f\n",
                                 * IRData_Remap_Latitude[YInc][XInc],
                                 * IRData_Remap_Longitude[YInc][XInc],
                                 * XOff,YOff,ProxyValueX,ProxyValueY,
                                 * SpiralValueX,SpiralValueY,
                                 * RawCrossScore,CrossScoreClean);
                                 */
                                NumPoints++;
                            }
                        }
                    }
                    /* calculate mean of all values in CrossScore array */
                    SpiralMeanCrossScore = CrossScoreSum / (double) NumPoints;
                    /* store location of maximum score position */
                    if (SpiralMeanCrossScore > SpiralMeanCrossMaxScore) {
                        SpiralMeanCrossMaxScore = SpiralMeanCrossScore;
                        SpiralMeanCrossMaxYLocation = YOff;
                        SpiralMeanCrossMaxXLocation = XOff;
                    }
                }
            }
        }

        /*
         * System.out.printf("course grid : y=%f x=%f max=%f\n",
         * SpiralMeanCrossMaxYLocation,SpiralMeanCrossMaxXLocation,
         * SpiralMeanCrossMaxScore);
         */

        /* FINE GRID */
        int SpiralCount = 1;
        double FineGridXMinimum = SpiralMeanCrossMaxXLocation - CourseGridSpacingDegree;
        double FineGridXMaximum = SpiralMeanCrossMaxXLocation + CourseGridSpacingDegree;
        double FineGridYMimimum = SpiralMeanCrossMaxYLocation - CourseGridSpacingDegree;
        double FineGridYMaximum = SpiralMeanCrossMaxYLocation + CourseGridSpacingDegree;
        SpiralMeanCrossMaxScore = -99.0;
        for (XOff = FineGridXMinimum; XOff <= FineGridXMaximum; XOff = XOff + FineGridSpacingDegree) {
            for (YOff = FineGridYMimimum; YOff <= FineGridYMaximum; YOff = YOff
                    + FineGridSpacingDegree) {
                /* XOff/YOff are offset coordinates from "starting point" */
                CrossScoreSum = 0.0;
                NumPoints = 0;
                for (YInc = 1; YInc < IRData_NormCoord_NumberRows - 2; YInc++) {
                    for (XInc = 1; XInc < IRData_NormCoord_NumberColumns - 2; XInc++) {
                        SearchRadius = Math.pow(IRData_NormCoord_Longitude[YInc][XInc], 2)
                                + Math.pow(IRData_NormCoord_Latitude[YInc][XInc], 2);
                        if (SearchRadius < FilterDiscSize) {
                            ProxyValueX = LongitudeMultFactor
                                    * IRData_NormCoord_Longitude[YInc][XInc] - XOff;
                            ProxyValueY = IRData_NormCoord_Latitude[YInc][XInc] - YOff;
                            DenominatorValue = Math.sqrt((AlphaPOWP1)
                                    * (Math.pow(ProxyValueX, 2) + Math.pow(ProxyValueY, 2)));
                            SpiralValueX = (Alpha * ProxyValueX + (SignFactor * ProxyValueY))
                                    / DenominatorValue;
                            SpiralValueY = (Alpha * ProxyValueY - (SignFactor * ProxyValueX))
                                    / DenominatorValue;
                            RawCrossScore = (SpiralValueX * NSTempGradientArray[YInc][XInc])
                                    - (SpiralValueY * EWTempGradientArray[YInc][XInc]);
                            CrossScoreClean = Math.max(0.0, -RawCrossScore)
                                    + (OutsideFactor * Math.max(0.0, RawCrossScore));
                            CrossScoreSum = CrossScoreSum + CrossScoreClean;
                            NumPoints++;
                        }
                    }
                }
                /* calculate mean of all values in CrossScore array */
                SpiralMeanCrossScore = CrossScoreSum / (double) NumPoints;
                /* store location of maximum score position */
                if (SpiralMeanCrossScore > SpiralMeanCrossMaxScore) {
                    SpiralMeanCrossMaxScore = SpiralMeanCrossScore;
                    SpiralMeanCrossMaxYLocation = YOff;
                    SpiralMeanCrossMaxXLocation = XOff;
                }
                SpiralCenterAnalysisField[SpiralCount][0] = SpiralMeanCrossScore;
                SpiralCenterAnalysisField[SpiralCount][1] = (double) YOff;
                SpiralCenterAnalysisField[SpiralCount][2] = (double) XOff;
                SpiralCenterAnalysisField[0][0] = (double) SpiralCount;
                SpiralCount++;
            }
        }

        /*
         * System.out.printf("fine grid : y=%f x=%f max=%f\n",
         * SpiralMeanCrossMaxYLocation
         * ,SpiralMeanCrossMaxXLocation,SpiralMeanCrossMaxScore);
         */

        SpiralCenterAnalysisField[0][1] = 0.0;
        SpiralCenterAnalysisField[0][2] = 0.0;

        /* determine lat/lon point from x/y coordinates */
        SpiralCenterLatitude = SpiralMeanCrossMaxYLocation + InputLatitude;
        SpiralCenterLongitude = ((LongitudeMultFactor * SpiralMeanCrossMaxXLocation) / (Math.cos(PI
                * InputLatitude / 180.0)))
                + InputLongitude;
        SpiralCenterScore = SpiralMeanCrossMaxScore;
        for (YInc = 1; YInc <= (int) SpiralCenterAnalysisField[0][0]; YInc++) {
            SpiralCenterAnalysisField[YInc][1] = SpiralCenterAnalysisField[YInc][1] + InputLatitude;
            SpiralCenterAnalysisField[YInc][2] = ((LongitudeMultFactor * SpiralCenterAnalysisField[YInc][2]) / (Math
                    .cos(PI * InputLatitude / 180.0))) + InputLongitude;
        }

        return new double[] { SpiralCenterLatitude, SpiralCenterLongitude, SpiralCenterScore };
    }

    private static void Gradient(double TemperatureInputArray[][], int ElementXNumber,
                                 int LineYNumber, double LongitudeIncrement, double LatitudeIncrement) {

        int XInc, YInc;

        /* initialize arrays */
        for (YInc = 0; YInc < LineYNumber - 1; YInc++) {
            for (XInc = 0; XInc < ElementXNumber - 1; XInc++) {
                NSTempGradientArray[YInc][XInc] = 0.0;
                EWTempGradientArray[YInc][XInc] = 0.0;
            }
        }

        for (YInc = 1; YInc < LineYNumber - 1; YInc++) {
            for (XInc = 1; XInc < ElementXNumber - 1; XInc++) {
                /* determine N-S gradient at point */
                NSTempGradientArray[YInc][XInc] = (TemperatureInputArray[YInc - 1][XInc] - TemperatureInputArray[YInc + 1][XInc])
                        / (2.0 * LatitudeIncrement);
                /* determine E-W gradient at point */
                EWTempGradientArray[YInc][XInc] = (TemperatureInputArray[YInc][XInc + 1] - TemperatureInputArray[YInc][XInc - 1])
                        / (2.0 * LongitudeIncrement);
            }
        }

    }

    private static double[] RingFit(double RingFitFirstGuessLatitude,
                                    double RingFitFirstGuessLongitude) {

        int XInc, YInc, ZInc;
        int CircleFilterRadii;
        int CircleFilterPoints;

        int RingWidthLocal = (int) RING_WIDTH;
        int XLocation = 0;
        int YLocation = 0;
        int CircleFilterXLocation = 0;
        int CircleFilterYLocation = 0;

        double[][] GradientFieldXDirection = new double[200][200];
        double[][] GradientFieldYDirection = new double[200][200];

        double RingFitSearchRadiusDegree = 0.75;
        double RingFitMinRadiusDegree = 0.06;
        double RingFitMaxRadiusDegree = 0.40;
        double CircleFilterDistance = 0.0;

        double DotScoreMaximum = -99999.0;
        double DotProductXDirection = -999.9;
        double DotProductYDirection = -999.9;
        double DotProductXYValue = -999.9;
        double SignFactor = 0.0;
        double DotScoreValue = 0.0;
        double DotScoreValueSum = 0.0;
        double DotScoreFinal = 0.0;
        double NANAdjustmentValue = 0.0;

        int ImageResolution = (int) Data.GetCurrentImageResolution();
        int IncAddVal = (ImageResolution > RingWidthLocal) ? 1
                : (RingWidthLocal - ImageResolution + 1);

        /* derive values */
        double DegreesPerPixel = Math
                .abs(IRData_Remap_Latitude[0][0] - IRData_Remap_Latitude[1][0]);
        int RingFitSearchRadiusPixel = (int) Math
                .round(RingFitSearchRadiusDegree / DegreesPerPixel);
        int RingFitMinRadiusPix = (int) Math.max(2.0,
                Math.round(RingFitMinRadiusDegree / DegreesPerPixel));
        int MaximumRadiusSizePixels = (int) Math.round(RingFitMaxRadiusDegree / DegreesPerPixel);

        double LongitudeIncrement = 1.0;
        double LatitudeIncrement = -1.0;

        /*
         * System.out.printf("RingFit: %d %f %d %d %d\n",IncAddVal,DegreesPerPixel
         * , RingFitSearchRadiusPixel,RingFitMinRadiusPix,
         * MaximumRadiusSizePixels);
         */
        /*
         * for(YInc=0;YInc<IRData_Remap_NumberRows-1;YInc++) {
         * for(XInc=0;XInc<IRData_Remap_NumberColumns-1;XInc++) {
         * System.out.printf
         * ("Y=%d X=%d lat=%f lon=%f\n",YInc,XInc,IRData_Remap_Latitude
         * [YInc][XInc],IRData_Remap_Longitude[YInc][XInc]); } }
         */

        /* NEED TO PASS BACK BOTH 2D ARRAYS FROM GRADIENT AND CIRCLEFILT */
        /* calculate gradient field */
        Gradient(IRData_Remap_Temperature, IRData_Remap_NumberColumns, IRData_Remap_NumberRows,
                LongitudeIncrement, LatitudeIncrement);

        for (YInc = 0; YInc < IRData_Remap_NumberRows - 1; YInc++) {
            for (XInc = 0; XInc < IRData_Remap_NumberColumns - 1; XInc++) {
                GradientFieldYDirection[YInc][XInc] = NSTempGradientArray[YInc][XInc];
                GradientFieldXDirection[YInc][XInc] = EWTempGradientArray[YInc][XInc];
                /*
                 * System.out.printf("GRADIENT  Y=%d X=%d Xdir=%f Ydir=%f  ",YInc
                 * ,
                 * XInc,GradientFieldXDirection[YInc][XInc],GradientFieldYDirection
                 * [YInc][XInc]);
                 */
                /*
                 * System.out.printf(" lat=%f lon=%f\n",IRData_Remap_Latitude[YInc
                 * ][XInc],IRData_Remap_Longitude[YInc][XInc]);
                 */
            }
        }
        /*
         * System.out.printf(
         * "RingFit : RingFitFirstGuessLatitude=%f RingFitFirstGuessLongitude=%f\n"
         * , RingFitFirstGuessLatitude,RingFitFirstGuessLongitude);
         */

        /* make matricies of row and column numbers */
        int Lalo2IndsReturn[] = Lalo2IndsFloat(RingFitFirstGuessLatitude,
                RingFitFirstGuessLongitude, IRData_Remap_Latitude, IRData_Remap_Longitude,
                IRData_Remap_NumberColumns, IRData_Remap_NumberRows);

        int FirstGuessXLocation = Lalo2IndsReturn[0];
        int FirstGuessYLocation = Lalo2IndsReturn[1];

        /*
         * System.out.printf(
         * "RingFit : FirstGuessXLocation=%d FirstGuessYLocation=%d\n",
         * FirstGuessXLocation,FirstGuessYLocation);
         */

        /* initialize circle/ring filter arrays */
        /* radius in pixels */

        for (CircleFilterRadii = 0; CircleFilterRadii < 100; CircleFilterRadii++) {
            /* number of points on circle at radius */
            for (CircleFilterPoints = 0; CircleFilterPoints < 2000; CircleFilterPoints++) {
                CircleFilterRowArray[CircleFilterRadii][CircleFilterPoints] = 0;
                CircleFilterColumnArray[CircleFilterRadii][CircleFilterPoints] = 0;
            }
        }

        /*
         * determine digital pixel coordinates for ring analysis for different
         * radii sizes
         */
        for (CircleFilterRadii = RingFitMinRadiusPix; CircleFilterRadii <= MaximumRadiusSizePixels; CircleFilterRadii++) {
            CircleFilt(CircleFilterRadii);
        }

        /* search image box */
        ZInc = 1;
        /* develop the accumulator */
        for (CircleFilterRadii = RingFitMinRadiusPix; CircleFilterRadii < MaximumRadiusSizePixels; CircleFilterRadii++) {
            int CircleFilterPointCount = CircleFilterRowArray[CircleFilterRadii][0];
            /* determine each main point in analysis disc */
            for (XInc = 1; XInc < IRData_Remap_NumberColumns - 1; XInc = XInc + IncAddVal) {
                for (YInc = 1; YInc < IRData_Remap_NumberRows - 1; YInc = YInc + IncAddVal) {
                    /*
                     * System.out.printf("XInc=%d YInc=%d  %d %d: %d\n",XInc,YInc
                     * , FirstGuessXLocation,FirstGuessYLocation,
                     * RingFitSearchRadiusPixel);
                     */

                    CircleFilterDistance = (double) ((XInc - FirstGuessXLocation) * (XInc - FirstGuessXLocation))
                            + ((YInc - FirstGuessYLocation) * (YInc - FirstGuessYLocation));
                    /*
                     * System.out.printf(
                     * "XInc=%d YInc=%d CircleFilterDistance=%f RingFitSearchRadiusPixel=%d\n"
                     * ,
                     * XInc,YInc,CircleFilterDistance,RingFitSearchRadiusPixel);
                     */
                    if (CircleFilterDistance <= (double) (RingFitSearchRadiusPixel * RingFitSearchRadiusPixel)) {
                        /*
                         * if main point (YInc,XInc) is in disc, calculate
                         * dotproduct for each subpoint on ring around main
                         * point
                         */
                        DotScoreValueSum = 0.0;
                        int NANCount = 0;
                        boolean FoundMoatRegionTF = false;
                        /*
                         * System.out.printf(
                         * "XInc=%d YInc=%d  CircleFilterPointCount=%d %d\n",
                         * XInc,YInc,CircleFilterPointCount,CircleFilterRadii);
                         */
                        for (CircleFilterPoints = 1; CircleFilterPoints <= CircleFilterPointCount; CircleFilterPoints++) {
                            /*
                             * System.out.printf(
                             * "CircleFilterPoints=%d CircleFilterRadii=%d XInc=%d YInc=%d\n"
                             * ,
                             * CircleFilterPoints,CircleFilterRadii,XInc,YInc);
                             */
                            CircleFilterXLocation = XInc
                                    + CircleFilterColumnArray[CircleFilterRadii][CircleFilterPoints];
                            CircleFilterYLocation = YInc
                                    + CircleFilterRowArray[CircleFilterRadii][CircleFilterPoints];
                            /*
                             * System.out.printf("%d : %d %d --  %d : %d %d ",
                             * CircleFilterYLocation,YInc,
                             * CircleFilterRowArray[CircleFilterRadii
                             * ][CircleFilterPoints],
                             * CircleFilterXLocation,XInc,
                             * CircleFilterColumnArray
                             * [CircleFilterRadii][CircleFilterPoints]);
                             */
                            if ((CircleFilterXLocation < 1) || (CircleFilterYLocation < 1)) {
                                DotProductXDirection = -999.9;
                                DotProductYDirection = -999.9;
                            } else {
                                /*
                                 * System.out.printf(
                                 * "I=%d x=%d  J=%d y=%d  : %b %b\n",
                                 * CircleFilterXLocation
                                 * ,IRData_Remap_NumberColumns,
                                 * CircleFilterYLocation
                                 * ,IRData_Remap_NumberRows,
                                 * (CircleFilterXLocation
                                 * >=IRData_Remap_NumberColumns),
                                 * (CircleFilterYLocation
                                 * >=IRData_Remap_NumberRows));
                                 */
                                if ((CircleFilterXLocation >= (IRData_Remap_NumberColumns - 1))
                                        || (CircleFilterYLocation >= (IRData_Remap_NumberRows - 1))) {
                                    DotProductXDirection = -999.9;
                                    DotProductYDirection = -999.9;
                                } else {
                                    if (MoatMaskFlagField[CircleFilterYLocation][CircleFilterXLocation] == 1) {
                                        FoundMoatRegionTF = true;
                                    }
                                    /*
                                     * System.out.printf(
                                     * " mask=%d  FoundMoatRegionTF=%b\n"
                                     * ,MoatMaskFlagField
                                     * [CircleFilterYLocation][
                                     * CircleFilterXLocation
                                     * ],FoundMoatRegionTF);
                                     */
                                    if (FoundMoatRegionTF) {
                                        DotProductXDirection = -999.9;
                                        DotProductYDirection = -999.9;
                                    } else {
                                        DotProductXDirection = ((double) CircleFilterRowArray[CircleFilterRadii][CircleFilterPoints] / (double) CircleFilterRadii)
                                                * GradientFieldYDirection[CircleFilterYLocation][CircleFilterXLocation];
                                        /*
                                         * System.out.printf(
                                         * " XX | %d %d j=%d i=%d j=%d i=%d  GradientFieldYDirection=%f  GradientFieldXDirection=%f | \n"
                                         * ,
                                         * CircleFilterPoints,CircleFilterRadii,
                                         * CircleFilterRowArray
                                         * [CircleFilterRadii
                                         * ][CircleFilterPoints],
                                         * CircleFilterColumnArray
                                         * [CircleFilterRadii
                                         * ][CircleFilterPoints],
                                         * CircleFilterYLocation,
                                         * CircleFilterXLocation,
                                         * GradientFieldYDirection
                                         * [CircleFilterYLocation
                                         * ][CircleFilterXLocation],
                                         * GradientFieldXDirection
                                         * [CircleFilterYLocation
                                         * ][CircleFilterXLocation]);
                                         */
                                        DotProductYDirection = ((double) CircleFilterColumnArray[CircleFilterRadii][CircleFilterPoints] / (double) CircleFilterRadii)
                                                * GradientFieldXDirection[CircleFilterYLocation][CircleFilterXLocation];
                                    }
                                }
                            }
                            if ((DotProductXDirection < -999.0) || (DotProductYDirection < -999.0)) {
                                NANCount++;
                            } else {
                                DotProductXYValue = DotProductXDirection + DotProductYDirection;
                                if (DotProductXYValue == 0.0) {
                                    SignFactor = 0.0;
                                } else {
                                    /* return -1/+1 for -/+ value */
                                    SignFactor = Math.abs(DotProductXYValue) / DotProductXYValue;
                                }
                                DotScoreValue = SignFactor
                                        * (Math.log(1.0 + Math.abs(DotProductXYValue)));
                                DotScoreValueSum = DotScoreValueSum + DotScoreValue;
                            }
                            /*
                             * System.out.printf(
                             * "dot product X=%f Y=%f XY=%f value=%f sum=%f\n"
                             * ,DotProductXDirection,
                             * DotProductYDirection,DotProductXYValue
                             * ,DotScoreValue,DotScoreValueSum);
                             */
                        } /* if indisk */
                        /*
                         * System.out.printf("dot product Final Sum=%f \n",
                         * DotScoreValueSum);
                         */
                        /*
                         * check for missing data and adjust DotScoreFinal
                         * accordingly
                         */
                        /*
                         * System.out.printf(
                         * "Y=%d X=%d foundmoat=%b  nancount=%f  xyz=%f\n"
                         * ,YInc,XInc
                         * ,FoundMoatRegionTF,(double)NANCount,0.575*(double
                         * )CircleFilterPointCount);
                         */
                        if (FoundMoatRegionTF
                                || ((double) NANCount) > (0.575 * (double) CircleFilterPointCount)) {
                            DotScoreFinal = 0.0;
                        } else {
                            NANAdjustmentValue = (double) CircleFilterPointCount
                                    / (double) (CircleFilterPointCount - NANCount);
                            DotScoreFinal = -NANAdjustmentValue * DotScoreValueSum
                                    / Math.sqrt((double) CircleFilterPointCount);
                        }
                        /*
                         * System.out.printf("XX final=%f maximum=%f ",DotScoreFinal
                         * ,DotScoreMaximum);
                         */
                        /*
                         * System.out.printf(
                         * "    circlefilterpointcount=%d sqrt=%f "
                         * ,CircleFilterPointCount
                         * ,Math.sqrt((double)CircleFilterPointCount));
                         */
                        if (DotScoreFinal > DotScoreMaximum) {
                            DotScoreMaximum = DotScoreFinal;
                            XLocation = XInc;
                            YLocation = YInc;
                            /*
                             * System.out.printf("   xloc=%d yloc=%d MaxRadSize=%d"
                             * ,XInc,YInc,MaximumRadiusSize);
                             */
                        }
                        /* System.out.printf("\n"); */
                        RingScoreAnalysisField[ZInc][0] = DotScoreFinal;
                        RingScoreAnalysisField[ZInc][1] = IRData_Remap_Latitude[YInc][XInc];
                        RingScoreAnalysisField[ZInc][2] = IRData_Remap_Longitude[YInc][XInc];
                        RingScoreAnalysisField[0][0] = (double) ZInc;
                        /*
                         * System.out.printf("ZZZZ  0=%f 1=%f 2=%f x=%f\n",
                         * RingScoreAnalysisField
                         * [ZInc][0],RingScoreAnalysisField[ZInc][1],
                         * RingScoreAnalysisField
                         * [ZInc][2],RingScoreAnalysisField[0][0]);
                         */
                        ZInc++;
                    }
                } /* XInc */
            } /* YInc */
        } /* CircleFilterRadii */

        RingScoreAnalysisField[0][1] = 0.0;
        RingScoreAnalysisField[0][2] = 0.0;

        /* make matricies of row and column numbers */
        /* System.out.printf("inds2lalo: x=%d y=%d \n",XLocation,YLocation); */
        double Inds2LaloReturn[] = Inds2LaloFloat(XLocation, YLocation, IRData_Remap_Latitude,
                IRData_Remap_Longitude, IRData_Remap_NumberColumns, IRData_Remap_NumberRows);
        double MaxScoreLatitude = Inds2LaloReturn[0];
        double MaxScoreLongitude = Inds2LaloReturn[1];

        double RingFitLatitude = MaxScoreLatitude;
        double RingFitLongitude = MaxScoreLongitude;
        double RingFitMaxScore = DotScoreMaximum;

        return new double[] { RingFitLatitude, RingFitLongitude, RingFitMaxScore };
    }

    private static int[] Lalo2IndsFloat(double LatitudeInput, double LongitudeInput,
                                        double LatitudeArrayInput[][], double LongitudeArrayInput[][], int ElementXNumber,
                                        int LineYNumber) {

        double LatitudeMinimum = LatitudeArrayInput[LineYNumber - 1][0];
        double LatitudeMaximum = LatitudeArrayInput[0][0];
        double LongitudeMinimum = LongitudeArrayInput[0][ElementXNumber - 1];
        double LongitudeMaximum = LongitudeArrayInput[0][0];

        int YAxisPositionReturn = (int) ((((double) LineYNumber - 1.0) / (LatitudeMaximum - LatitudeMinimum)) * (LatitudeMaximum - LatitudeInput));
        int XAxisPositionReturn = (int) ((((double) ElementXNumber - 1.0) / (LongitudeMaximum - LongitudeMinimum)) * (LongitudeMaximum - LongitudeInput));

        return new int[] { XAxisPositionReturn, YAxisPositionReturn };

    }

    private static double[] Inds2LaloFloat(int XAxisPosition, int YAxisPosition,
                                           double LatitudeArrayInput[][], double LongitudeArrayInput[][], int ElementXNumber,
                                           int LineYNumber) {

        double LatitudeMinimum = LatitudeArrayInput[LineYNumber - 1][0];
        double LatitudeMaximum = LatitudeArrayInput[0][0];
        double LongitudeMinimum = LongitudeArrayInput[0][ElementXNumber - 1];
        double LongitudeMaximum = LongitudeArrayInput[0][0];

        double LongitudeReturn = LongitudeMaximum
                - ((((double) XAxisPosition) / ((double) ElementXNumber - 1.0)) * (LongitudeMaximum - LongitudeMinimum));
        double LatitudeReturn = LatitudeMaximum
                - ((((double) YAxisPosition) / ((double) LineYNumber - 1.0)) * (LatitudeMaximum - LatitudeMinimum));

        return new double[] { LatitudeReturn, LongitudeReturn };

    }

    private static void CircleFilt(int RingRadiusInput) {

        int XInc, YInc;
        int PointCount = 1;
        int RadiusPlus1 = RingRadiusInput + 1;

        double ThresholdDifference = 0.5 * (double) (((RingRadiusInput + 1) * (RingRadiusInput + 1)) - (RingRadiusInput * RingRadiusInput));
        for (XInc = -RadiusPlus1; XInc <= RadiusPlus1; XInc++) {
            for (YInc = -RadiusPlus1; YInc <= RadiusPlus1; YInc++) {
                double DifferenceValue = (double) ((YInc * YInc) + (XInc * XInc) - (RingRadiusInput * RingRadiusInput));
                if (Math.abs(DifferenceValue) <= ThresholdDifference) {
                    CircleFilterRowArray[RingRadiusInput][PointCount] = YInc;
                    CircleFilterColumnArray[RingRadiusInput][PointCount] = XInc;
                    PointCount++;
                }
            }
        }

        /* number of points on given radius size */
        CircleFilterRowArray[RingRadiusInput][0] = PointCount - 1;
        CircleFilterColumnArray[RingRadiusInput][0] = PointCount - 1;

    }

    private static void MoatMaskCalc(double MoatMaskTempThreshold, double RingFitMaxRadiusDegree,
                                     int MoatSignCheckFlag) {

        int ArrayInc, XInc, YInc;
        int NumberColumns = IRData_Remap_NumberColumns;
        int NumberRows = IRData_Remap_NumberRows;

        /* initialize arrays */
        for (YInc = 0; YInc < NumberRows; YInc++) {
            for (XInc = 0; XInc < NumberColumns; XInc++) {
                MoatMaskFlagField[YInc][XInc] = 0;
                BlackWhiteFieldArray[YInc][XInc] = 0;
            }
        }

        MeshGrid(MoatMaskTempThreshold, MoatSignCheckFlag);
        int NumberOfObjects = BWImage(8, NumberRows, NumberColumns);

        for (ArrayInc = 1; ArrayInc <= NumberOfObjects; ArrayInc++) {
            double LatitudeMaximum = -90.0;
            double LatitudeMinimum = 90.0;
            double LongitudeMaximum = -180.0;
            double LongitudeMinimum = 180.0;
            for (YInc = 0; YInc < NumberRows; YInc++) {
                for (XInc = 0; XInc < NumberColumns; XInc++) {
                    if (MoatMaskFlagField[YInc][XInc] == ArrayInc) {
                        double LatitudeValue = IRData_Remap_Latitude[YInc][XInc];
                        double LongitudeValue = IRData_Remap_Longitude[YInc][XInc];
                        if (LatitudeValue > LatitudeMaximum) {
                            LatitudeMaximum = LatitudeValue;
                        }
                        if (LongitudeValue > LongitudeMaximum) {
                            LongitudeMaximum = LongitudeValue;
                        }
                        if (LatitudeValue < LatitudeMinimum) {
                            LatitudeMinimum = LatitudeValue;
                        }
                        if (LongitudeValue < LongitudeMinimum) {
                            LongitudeMinimum = LongitudeValue;
                        }
                    }
                }
            }
            double AverageLatitude = .5 * (LatitudeMinimum + LatitudeMaximum);
            double FeatureLength = Math.sqrt(Math.pow((LatitudeMaximum - LatitudeMinimum), 2)
                    + Math.pow(
                    (LongitudeMaximum - LongitudeMinimum)
                            / Math.cos((PI * AverageLatitude) / 180.0), 2));
            int MoatFlagID = (FeatureLength > (RingFitMaxRadiusDegree * 2.0)) ? 1 : 0;
            for (YInc = 0; YInc < NumberRows; YInc++) {
                for (XInc = 0; XInc < NumberColumns; XInc++) {
                    if (MoatMaskFlagField[YInc][XInc] == ArrayInc) {
                        MoatMaskFlagField[YInc][XInc] = MoatFlagID;
                    }
                }
            }
        }

    }

    private static void MeshGrid(double TemperatureThresholdValue, int MoatSignCheckFlag) {

        int XInc, YInc;

        for (XInc = 0; XInc < IRData_Remap_NumberColumns; XInc++) {
            for (YInc = 0; YInc < IRData_Remap_NumberRows; YInc++) {
                if (MoatSignCheckFlag == 1) {
                    BlackWhiteFieldArray[YInc][XInc] = (IRData_Remap_Temperature[YInc][XInc] > TemperatureThresholdValue) ? 1
                            : 0;
                } else {
                    BlackWhiteFieldArray[YInc][XInc] = (IRData_Remap_Temperature[YInc][XInc] < TemperatureThresholdValue) ? 1
                            : 0;
                }
            }
        }

    }

    private static int BWImage(int ConnectednessValue, int NumberRows, int NumberColumns) {
        /*
         * blackwhitefieldarray = BooleanValueArray; moatmaskflagfield =
         * LabelImageArray
         */

        int IncVal, XInc, YInc;
        int B_Value, C_Value, D_Value, E_Value;
        int TableElements = 0;
        int LabelValue = 0;
        int[] LabelSetArray = new int[NumberRows * NumberColumns];

        LabelSetArray[0] = 0;

        for (YInc = 0; YInc < NumberRows; YInc++) {
            for (XInc = 0; XInc < NumberColumns; XInc++) {
                if (BlackWhiteFieldArray[YInc][XInc] == 1) {
                    /* if A is an object */
                    /*
                     * get the neighboring pixels B_Value, C_Value, D_Value, and
                     * E_Value
                     */
                    B_Value = 0;
                    C_Value = 0;
                    D_Value = 0;
                    E_Value = 0;
                    if (XInc > 0) {
                        B_Value = Find(LabelSetArray, MoatMaskFlagField[YInc][XInc - 1]);
                    }
                    if (YInc > 0) {
                        C_Value = Find(LabelSetArray, MoatMaskFlagField[YInc - 1][XInc]);
                    }
                    if ((YInc > 0) && (XInc > 0)) {
                        D_Value = Find(LabelSetArray, MoatMaskFlagField[YInc - 1][XInc - 1]);
                    }
                    if ((YInc > 0) && (XInc > (NumberColumns - 1))) {
                        E_Value = Find(LabelSetArray, MoatMaskFlagField[YInc - 1][XInc + 1]);
                    }
                    if (ConnectednessValue == 4) {
                        /* apply 4 connectedness */
                        if ((B_Value != 0) && (C_Value != 0)) {
                            /* B_Value and C_Value are labeled */
                            if (B_Value == C_Value) {
                                MoatMaskFlagField[YInc][XInc] = B_Value;
                            } else {
                                LabelSetArray[C_Value] = B_Value;
                                MoatMaskFlagField[YInc][XInc] = B_Value;
                            }
                        } else if (B_Value != 0) {
                            /* B_Value is object but C_Value is not */
                            MoatMaskFlagField[YInc][XInc] = B_Value;
                        } else if (C_Value != 0) {
                            /* C_Value is object but B_Value is not */
                            MoatMaskFlagField[YInc][XInc] = C_Value;
                        } else {
                            /* B_Value, C_Value, D_Value not object - new object */
                            /* label and put into table */
                            TableElements++;
                            LabelSetArray[TableElements] = TableElements;
                            MoatMaskFlagField[YInc][XInc] = LabelSetArray[TableElements];
                        }
                    } else if (ConnectednessValue == 6) {
                        /* apply 6 connected ness */
                        if (D_Value != 0) {
                            /* D_Value object, copy label and move on */
                            MoatMaskFlagField[YInc][XInc] = D_Value;
                        } else if ((B_Value != 0) && (C_Value != 0)) {
                            /* B_Value and C_Value are labeled */
                            if (B_Value == C_Value) {
                                MoatMaskFlagField[YInc][XInc] = B_Value;
                            } else {
                                LabelValue = Math.min(B_Value, C_Value);
                                LabelSetArray[B_Value] = LabelValue;
                                LabelSetArray[C_Value] = LabelValue;
                                MoatMaskFlagField[YInc][XInc] = LabelValue;
                            }
                        } else if (B_Value != 0) {
                            /* B_Value is object but C_Value is not */
                            MoatMaskFlagField[YInc][XInc] = B_Value;
                        } else if (C_Value != 0) {
                            /* C_Value is object but B_Value is not */
                            MoatMaskFlagField[YInc][XInc] = C_Value;
                        } else {
                            /* B_Value, C_Value, D_Value not object - new object */
                            /* label and put into table */
                            TableElements++;
                            LabelSetArray[TableElements] = TableElements;
                            MoatMaskFlagField[YInc][XInc] = LabelSetArray[TableElements];
                        }
                    } else if (ConnectednessValue == 8) {
                        /* apply 8 connectedness */
                        if ((B_Value != 0) || (C_Value != 0) || (D_Value != 0) || (E_Value != 0)) {
                            LabelValue = B_Value;
                            if (B_Value != 0) {
                                LabelValue = B_Value;
                            } else if (C_Value != 0) {
                                LabelValue = C_Value;
                            } else if (D_Value != 0) {
                                LabelValue = D_Value;
                            } else {
                                LabelValue = E_Value;
                            }
                            MoatMaskFlagField[YInc][XInc] = LabelValue;
                            if ((B_Value != 0) && (B_Value != LabelValue)) {
                                LabelSetArray[B_Value] = LabelValue;
                            }
                            if ((C_Value != 0) && (C_Value != LabelValue)) {
                                LabelSetArray[C_Value] = LabelValue;
                            }
                            if ((D_Value != 0) && (D_Value != LabelValue)) {
                                LabelSetArray[D_Value] = LabelValue;
                            }
                            if ((E_Value != 0) && (E_Value != LabelValue)) {
                                LabelSetArray[E_Value] = LabelValue;
                            }
                        } else {
                            /* label and put into table */
                            TableElements++;
                            LabelSetArray[TableElements] = TableElements;
                            MoatMaskFlagField[YInc][XInc] = LabelSetArray[TableElements];
                        }
                    }
                } else {
                    /* A is not an object so leave it */
                    MoatMaskFlagField[YInc][XInc] = 0;
                }
            }
        }

        /* consolidate component table */
        for (IncVal = 0; IncVal <= TableElements; IncVal++) {
            LabelSetArray[IncVal] = Find(LabelSetArray, IncVal);
        }

        /* run image through the look-up table */
        for (YInc = 0; YInc < NumberRows; YInc++) {
            for (XInc = 0; XInc < NumberColumns; XInc++) {
                MoatMaskFlagField[YInc][XInc] = LabelSetArray[MoatMaskFlagField[YInc][XInc]];
            }
        }

        /* count up the objects in the image */
        for (IncVal = 0; IncVal <= TableElements; IncVal++) {
            LabelSetArray[IncVal] = 0;
        }

        for (YInc = 0; YInc < NumberRows; YInc++) {
            for (XInc = 0; XInc < NumberColumns; XInc++) {
                LabelSetArray[MoatMaskFlagField[YInc][XInc]]++;
            }
        }

        /* number the objects from 1 through n objects */
        int NumberObjects = 0;
        LabelSetArray[0] = 0;
        for (IncVal = 1; IncVal <= TableElements; IncVal++) {
            if (LabelSetArray[IncVal] > 0) {
                LabelSetArray[IncVal] = ++NumberObjects;
            }
        }

        /* run through the look-up table again */
        for (YInc = 0; YInc < NumberRows; YInc++) {
            for (XInc = 0; XInc < NumberColumns; XInc++) {
                MoatMaskFlagField[YInc][XInc] = LabelSetArray[MoatMaskFlagField[YInc][XInc]];
            }
        }

        return NumberObjects;

    }

    private static int Find(int InputArray[], int InputValue) {
        int IncVal = InputValue;

        while (InputArray[IncVal] != IncVal) {
            IncVal = InputArray[IncVal];
        }

        return IncVal;
    }

    /**
     * This routine will determine the confidence scores for the spiral fitting
     * and ring fitting routines and calculate the best possible position for
     * the storm center based upon a combination of the two methods, if
     * available.
     *
     * If the ring fitting routine does not determine a good candidate position,
     * the spiral fitting routine will be used alone. If the spiral fitting
     * routine candidate point is not "good", the forecast point will be
     * selected.
     *
     * @param FirstGuessLatitude
     *            First Guess latitude.
     * @param FirstGuessLongitude
     *            First Guess longitude.
     * @param SpiralCenterLatitude
     *            Spiral Analysis latitude at max location.
     * @param SpiralCenterLongitude
     *            Spiral Analysis longitude at max location.
     * @param SpiralCenterScoreValue
     *            Spiral Analysis Score value.
     * @param RingFitLatitude
     *            Ring Analysis latitude at max score location.
     * @param RingFitLongitude
     *            Ring Analysis longitude at max score location.
     * @param RingFitScoreValue
     *            Ring Analysis Score value.
     *
     * @return Array of four double values. The values represent: latitude of
     *         final selected location, longitude of final selected location,
     *         confidence score of final selected location, and the
     *         {@literal "method"} used to determine the final selected
     *         location. Possible values for the method: 1 for first guess, 2
     *         for enhanced spiral analysis, and 5 for combo ring/spiral
     *         analysis.
     */
    private static double[] CalcScores(double FirstGuessLatitude, double FirstGuessLongitude,
                                       double SpiralCenterLatitude, double SpiralCenterLongitude,
                                       double SpiralCenterScoreValue, double RingFitLatitude, double RingFitLongitude,
                                       double RingFitScoreValue) {
        int YInc;
        int FinalLocationSelectionMethodReturn = 0;
        int NumberOfSpirals = (int) SpiralCenterAnalysisField[0][0] + 1;

        double[] DistancePenaltyArray = new double[NumberOfSpirals];
        double[] InitialSpiralScoreArray = new double[NumberOfSpirals];
        double[] DistanceFromSpiralCenterArray = new double[NumberOfSpirals];
        double[] DistanceBonusArray = new double[NumberOfSpirals];
        double[] EnhancedSpiralScoreArray = new double[NumberOfSpirals];
        double[] FinalSpiralScoreArray = new double[NumberOfSpirals];

        double FinalLatitudeReturn = -99.99;
        double FinalLongitudeReturn = -999.99;
        double FinalScoreReturn = 0.0;
        double DistanceValue;
        double MaximumForecastErrorDegree = 1.0; /*
         * max dist between fcst and
         * final
         */
        double ExpectedMaximumForecastErrorDegree = MaximumForecastErrorDegree; /*
         * expected
         * error
         */

        /* max displacement */
        double MaximumAllowableDisplacement = ExpectedMaximumForecastErrorDegree * 1.15;

        /* Spiral Center analysis weight */
        double SPIRALWEIGHT = 10.0;

        /* RF distance penalty */
        double DISTPENALTYWEIGHT = (0.5 / ExpectedMaximumForecastErrorDegree);

        /* Ring Fit bonus value */
        double PROXIMITYBONUS = 4.5;

        /* RF bonus value threshold dist deg */
        double PROXIMITYTHRESH = 0.25;

        /* combination score threshold value */
        double COMBOSCORETHRESH = 15.0;

        /* convert distance in km to degrees */
        double KMperDegree = 111.0;

        double SpiralCenterIndexMaximum = -999.99;
        double SpiralCenterMaximumLatitude = -99.99;
        double SpiralCenterMaximumLongitude = -999.99;
        double SpiralMaximumDistanceFromGuess = 999.99;
        double IntermediateRingScore = 0.0;
        double FinalSpiralScoreValue = 0.0;
        double MaximumSpiralScoreValue = -99.99;
        double MaximumSpiralScoreLatitude = -99.99;
        double MaximumSpiralScoreLongitude = -999.99;

        /* Spiral Score Calculations */
        double LocalValue[] = Functions.distance_angle(FirstGuessLatitude, FirstGuessLongitude,
                SpiralCenterLatitude, SpiralCenterLongitude, 1);
        DistanceValue = LocalValue[0];
        double InitialSpiralScoreArrayScore = SpiralCenterScoreValue;

        for (YInc = 1; YInc < NumberOfSpirals; YInc++) {
            double LocalValue1[] = Functions.distance_angle(FirstGuessLatitude,
                    FirstGuessLongitude, SpiralCenterAnalysisField[YInc][1],
                    SpiralCenterAnalysisField[YInc][2], 1);
            DistanceValue = LocalValue1[0];
            DistancePenaltyArray[YInc] = -DISTPENALTYWEIGHT * (DistanceValue / KMperDegree);
            InitialSpiralScoreArray[YInc] = SPIRALWEIGHT
                    * (SpiralCenterAnalysisField[YInc][0] - InitialSpiralScoreArrayScore);
            double LocalValue2[] = Functions.distance_angle(SpiralCenterLatitude,
                    SpiralCenterLongitude, SpiralCenterAnalysisField[YInc][1],
                    SpiralCenterAnalysisField[YInc][2], 1);
            DistanceValue = LocalValue2[0];
            DistanceFromSpiralCenterArray[YInc] = DistanceValue / KMperDegree;
            if (DistanceFromSpiralCenterArray[YInc] <= PROXIMITYTHRESH) {
                DistanceBonusArray[YInc] = PROXIMITYBONUS;
            } else {
                DistanceBonusArray[YInc] = 0.0;
            }
            EnhancedSpiralScoreArray[YInc] = InitialSpiralScoreArray[YInc]
                    + DistancePenaltyArray[YInc];
            /*
             * System.out.printf(
             * "SpiralCenterAnalysisField :   YInc: %d  Lat: %f  Lon: %f Value: %f "
             * ,YInc,SpiralCenterAnalysisField[YInc][1],
             * SpiralCenterAnalysisField
             * [YInc][2],SpiralCenterAnalysisField[YInc][0]);
             */
            /*
             * System.out.printf(
             * " distPentaly=%f  DistanceFromSpiralCenterArray=%f DistanceBonusArray=%f   max=%f\n"
             * , DistancePenaltyArray[YInc],DistanceFromSpiralCenterArray[YInc],
             * DistanceBonusArray[YInc],SpiralCenterIndexMaximum);
             */
            if (EnhancedSpiralScoreArray[YInc] > SpiralCenterIndexMaximum) {
                SpiralCenterIndexMaximum = EnhancedSpiralScoreArray[YInc];
                SpiralCenterMaximumLatitude = SpiralCenterAnalysisField[YInc][1];
                SpiralCenterMaximumLongitude = SpiralCenterAnalysisField[YInc][2];
            }
            FinalSpiralScoreArray[YInc] = InitialSpiralScoreArray[YInc]
                    + DistancePenaltyArray[YInc] + DistanceBonusArray[YInc];
        }
        /*
         * System.out.printf(
         * "FirstGuessLatitude=%f  FirstGuessLongitude=%f SpiralCenterMaximumLatitude=%f SpiralCenterMaximumLongitude=%f\n"
         * , FirstGuessLatitude,FirstGuessLongitude,SpiralCenterMaximumLatitude,
         * SpiralCenterMaximumLongitude);
         */
        double LocalValue3[] = Functions.distance_angle(FirstGuessLatitude, FirstGuessLongitude,
                SpiralCenterMaximumLatitude, SpiralCenterMaximumLongitude, 1);
        DistanceValue = LocalValue3[0];
        SpiralMaximumDistanceFromGuess = DistanceValue / KMperDegree;
        /*
         * System.out.printf(
         * "SpiralMaximumDistanceFromGuess=%f MaximumAllowableDisplacement=%f\n"
         * , SpiralMaximumDistanceFromGuess,MaximumAllowableDisplacement);
         */
        if (SpiralMaximumDistanceFromGuess <= MaximumAllowableDisplacement) {
            /* Ring Score Calculations */
            double LocalValue4[] = Functions.distance_angle(FirstGuessLatitude,
                    FirstGuessLongitude, RingFitLatitude, RingFitLongitude, 1);
            DistanceValue = LocalValue4[0];
            for (YInc = 1; YInc < NumberOfSpirals; YInc++) {
                double[] RingScoreReturn = FindRingScore(SpiralCenterAnalysisField[YInc][1],
                        SpiralCenterAnalysisField[YInc][2], RingScoreAnalysisField);
                int RetErr = (int) RingScoreReturn[0];
                IntermediateRingScore = RingScoreReturn[1];
                if (RetErr == 1) {
                    FinalSpiralScoreValue = FinalSpiralScoreArray[YInc] + IntermediateRingScore;
                    /*
                     * System.out.printf(
                     * "    FOUND  NumberOfSpirals=%d  lat=%f  lon=%f ringScore=%f  FinalSpiralScoreValue=%f MaximumSpiralScoreValue=%f\n"
                     * , YInc,SpiralCenterAnalysisField[YInc][1],
                     * SpiralCenterAnalysisField[YInc][2],
                     * IntermediateRingScore,
                     * FinalSpiralScoreValue,MaximumSpiralScoreValue);
                     */
                    if (FinalSpiralScoreValue > MaximumSpiralScoreValue) {
                        MaximumSpiralScoreValue = FinalSpiralScoreValue;
                        MaximumSpiralScoreLatitude = SpiralCenterAnalysisField[YInc][1];
                        MaximumSpiralScoreLongitude = SpiralCenterAnalysisField[YInc][2];
                    }
                }
            }
            /*
             * System.out.printf(
             * "MaximumSpiralScoreValue=%f RingPart=%f SpiralCenterIndexMaximum=%f\n"
             * , MaximumSpiralScoreValue,IntermediateRingScore,
             * SpiralCenterIndexMaximum);
             */
            if (MaximumSpiralScoreValue >= COMBOSCORETHRESH) {
                /* use Combo Method */
                FinalScoreReturn = MaximumSpiralScoreValue;
                FinalLatitudeReturn = MaximumSpiralScoreLatitude;
                FinalLongitudeReturn = MaximumSpiralScoreLongitude;
                FinalLocationSelectionMethodReturn = 5;
            } else {
                /* use ESP method */
                FinalScoreReturn = 1.0 + SpiralCenterIndexMaximum;
                FinalLatitudeReturn = SpiralCenterMaximumLatitude;
                FinalLongitudeReturn = SpiralCenterMaximumLongitude;
                FinalLocationSelectionMethodReturn = 4;
            }
        } else {
            /* use Forecast Position */
            FinalScoreReturn = 0.0;
            FinalLatitudeReturn = FirstGuessLatitude;
            FinalLongitudeReturn = FirstGuessLongitude;
            FinalLocationSelectionMethodReturn = 1;
        }

        return new double[] { FinalLatitudeReturn, FinalLongitudeReturn, FinalScoreReturn,
                (double) FinalLocationSelectionMethodReturn };
    }

    /**
     * Find Ring score at selected location (spiral analysis location)
     *
     * @param FirstGuessLatitude
     *            Latitude of search location.
     * @param FirstGuessLongitude
     *            Longitude of search location.
     * @param RingScoreAnalysisField
     *            - Array/Grid of Ring Analysis scores.
     *
     * @return Array of two doubles. The first value will be either -1 (if
     *         nothing was found) or 1 (if found). The second value is the value
     *         at search location in ring analysis grid (if the first value is
     *         1).
     */
    private static double[] FindRingScore(double FirstGuessLatitude, double FirstGuessLongitude,
                                          double RingScoreAnalysisField[][]) {
        int YInc = 1;
        int Ret_ID = -1;
        double MaxRingDistance = RingScoreAnalysisField[0][0];
        double RingFixScoreReturn = -99.9;

        double LatitudeIncrement = Math.abs(IRData_Remap_Latitude[0][0]
                - IRData_Remap_Latitude[1][0]) / 2.0;
        double LongitudeIncrement = Math.abs(IRData_Remap_Longitude[0][1]
                - IRData_Remap_Longitude[0][0]) / 2.0;
        while (YInc <= MaxRingDistance) {
            if ((Math.abs(RingScoreAnalysisField[YInc][1] - FirstGuessLatitude) <= LatitudeIncrement)
                    && (Math.abs(RingScoreAnalysisField[YInc][2] - FirstGuessLongitude) <= LongitudeIncrement)) {
                if (RingScoreAnalysisField[YInc][0] > RingFixScoreReturn) {
                    RingFixScoreReturn = RingScoreAnalysisField[YInc][0];
                }
                Ret_ID = 1;
            }
            YInc++;
        }

        return new double[] { (double) Ret_ID, RingFixScoreReturn };
    }

    /**
     * Determine method location scheme to use by examining various
     * empirically-defined confidence factors.
     *
     * Confidence factors will be derived, with the "most confident" value used
     * as the final automatically determined storm position.
     *
     * @param ForecastLatitude
     *            NHC/JTWC interpolated latitude position.
     *
     * @param ForecastLongitude
     *            NHC/JTWC interpolated longitude position.
     *
     * @param RingSpiralLatitude
     *            Ring/Spiral Analysis latitude position.
     *
     * @param RingSpiralLongitude
     *            Ring/Spiral Analysis longitude position.
     *
     * @param RingSpiralScore
     *            Ring/Spiral Analysis confidence factor score.
     *
     * @param RingSpiralSelectionIDValue
     *            Ring/Spiral Analysis position derivation method.
     *
     * @return Array of three doubles. First value is latitude to be used,
     *         second value is longitude to be used, and the third value is the
     *         method used to determine storm position values.
     */
    private static double[] PickFinalLocation(int InputPositioningID, double ForecastLatitude,
                                              double ForecastLongitude, double RingSpiralLatitude, double RingSpiralLongitude,
                                              double RingSpiralScore, int RingSpiralSelectionIDValue) {

        int PositioningMethodID = 0;
        int HistoryRecEyeScene;
        int HistoryRecCloudScene;
        double FinalLatitude = -99.99;
        double FinalLongitude = -99.99;
        double FinalScoreValue = 0.0;
        double HistoryRecFinalTno = 0.0;
        double MaximumHistoryRecCI = 0.0;
        double HistoryRecRawTno = 0.0;
        double HistoryRecCI = 0.0;
        boolean ForceAutoFixUseTF_forTesting = false;
        boolean FoundEyeSceneTF = false;

        int CurDate = History.IRCurrentRecord.date;
        int CurTime = History.IRCurrentRecord.time;
        int CurCloudScene = History.IRCurrentRecord.cloudscene;
        int CurEyeScene = History.IRCurrentRecord.eyescene;
        double CurrentTime = Functions.calctime(CurDate, CurTime);

        int NumRecsHistory = History.HistoryNumberOfRecords();
        double InitStrengthValue = Env.InitRawTValue;
        boolean LandFlagTF = Env.LandFlagTF;

        int EyeSceneCount = 0;

        if ((Main.HistoryFileName != null) || (ForceAutoFixUseTF_forTesting)) {
            HistoryRecFinalTno = 9.0;
            MaximumHistoryRecCI = 9.0;
            HistoryRecRawTno = 9.0;
        } else if (NumRecsHistory == 0) {
            HistoryRecFinalTno = InitStrengthValue;
            MaximumHistoryRecCI = InitStrengthValue;
            HistoryRecRawTno = InitStrengthValue;
        } else {
            EyeSceneCount = 0;
            HistoryRecFinalTno = History.IRCurrentRecord.Traw;
            HistoryRecRawTno = History.IRCurrentRecord.Traw;
            MaximumHistoryRecCI = History.IRCurrentRecord.Traw;
            int XInc = 0;
            while (XInc < NumRecsHistory) {
                int RecDate = History.HistoryFile[XInc].date;
                int RecTime = History.HistoryFile[XInc].time;
                int RecLand = History.HistoryFile[XInc].land;
                double RecTnoRaw = History.HistoryFile[XInc].Traw;
                double HistoryRecTime = Functions.calctime(RecDate, RecTime);
                boolean LandCheckTF = true;
                if (((LandFlagTF) && (RecLand == 1)) || (RecTnoRaw < 1.0)) {
                    LandCheckTF = false;
                }
                if ((HistoryRecTime < CurrentTime) && (LandCheckTF)) {
                    HistoryRecCI = History.HistoryFile[XInc].CI;
                    HistoryRecFinalTno = History.HistoryFile[XInc].Tfinal;
                    HistoryRecRawTno = History.HistoryFile[XInc].Traw;
                    HistoryRecEyeScene = History.HistoryFile[XInc].eyescene;
                    HistoryRecCloudScene = History.HistoryFile[XInc].cloudscene;
                    if (HistoryRecCI > MaximumHistoryRecCI) {
                        MaximumHistoryRecCI = HistoryRecCI;
                    }
                    if ((HistoryRecEyeScene < 3)
                            || ((HistoryRecCloudScene == 1) && (HistoryRecEyeScene == 3))) {
                        EyeSceneCount = EyeSceneCount + 1;
                        /* checking for eye or embedded center scene types */
                        if (EyeSceneCount == 3) {
                            FoundEyeSceneTF = true;
                        }
                    } else {
                        EyeSceneCount = 0;
                    }
                }
                XInc++;
            }
            if (((CurEyeScene < 3) || ((CurCloudScene == 1) && (CurEyeScene == 3)))
                    && (EyeSceneCount == 2)) {
                FoundEyeSceneTF = true;
            }
        }

        /* System.out.printf("%d %d : ",CurDate,CurTime); */
        /*
         * System.out.printf("RingSpiralScore=%f HistoryRecFinalTno=%f\n",
         * RingSpiralScore,HistoryRecFinalTno);
         */
        /*
         * System.out.printf("HistoryRecRawTno=%f MaximumHistoryRecCI=%f\n",
         * HistoryRecRawTno,MaximumHistoryRecCI);
         */

        /*
         * check score for developing systems (MaximumHistoryRecCI<5.0) starting
         * at 3.0 or check score for weakeining systems
         * (MaximumHistoryRecCI>=5.0) only above 3.5
         */
        if (((HistoryRecRawTno >= 3.0) && (MaximumHistoryRecCI < 5.0))
                || ((HistoryRecRawTno >= 3.5) && (MaximumHistoryRecCI >= 5.0))) {

            if ((HistoryRecFinalTno <= 4.5) && (RingSpiralScore < 1.0) && (!FoundEyeSceneTF))
                RingSpiralScore = -99.99;

            /* System.out.printf(" FINALRingSpiralScore=%f ",RingSpiralScore); */

            if (RingSpiralScore > 0.0) {
                /* use Spiral/Ring methodology for center point */
                FinalLatitude = RingSpiralLatitude;
                FinalLongitude = RingSpiralLongitude;
                FinalScoreValue = RingSpiralScore;
                PositioningMethodID = RingSpiralSelectionIDValue;
            } else {
                /* CDO... can't find anything to focus on */
                FinalLatitude = ForecastLatitude;
                FinalLongitude = ForecastLongitude;
                FinalScoreValue = 0.0;
                PositioningMethodID = InputPositioningID;
            }
        } else {
            /*
             * current Tfinal is less than 3.5 or current scene is not an eye or
             * embedded center WILL USE FORECAST POSITION FOR AODT ANALYSIS
             */
            FinalLatitude = ForecastLatitude;
            FinalLongitude = ForecastLongitude;
            FinalScoreValue = 0.0;
            PositioningMethodID = InputPositioningID;
        }

        return new double[] { FinalLatitude, FinalLongitude, FinalScoreValue,
                (double) PositioningMethodID };

    }

    /**
     * Calls routines to setup transformation, transform, data move.
     *
     * Input data provided with global variable arrays containing original and
     * transformed arrays.
     */
    private static void RemapData() {
        /*
         * The following routines were originally developed by Dave Santek of
         * UW/SSEC and were added to the ADT under permission. If executed, an
         * array of latitude and longitude position arrays will be remapped to a
         * rectilinear projection for Tony Wimmers routines
         */

        int NumberOfCorners = 0;
        int LineSplineValue = 3;
        int ElementSplineValue = LineSplineValue;

        tiff_vars.in_elems = Data.IRData_NumberColumns;
        tiff_vars.in_lines = Data.IRData_NumberRows;

        /*
         * System.out.printf("elems=%d lines=%d\n",
         * tiff_vars.in_elems,tiff_vars.in_lines);
         */

        /* Uinit( ); */
        /*
         * dis_vars.xrectl = (double)tiff_vars.in_lines; dis_vars.xrecte =
         * (double)tiff_vars.in_elems;
         * System.out.printf("xrectl=%f xrecte=%f\n",
         * dis_vars.xrectl,dis_vars.xrecte);
         */

        DetermineDest();
        /* System.out.printf("after determineDest\n"); */

        NumberOfCorners = Init(LineSplineValue, ElementSplineValue);

        System.out.printf("number of corners=%d\n", NumberOfCorners);
        Corner(NumberOfCorners, LineSplineValue, ElementSplineValue);
        /* System.out.printf("after corners\n"); */

        if ((ElementSplineValue > 1) || (LineSplineValue > 1)) {
            DoMap(NumberOfCorners, LineSplineValue, ElementSplineValue);
        }

    }

    /**
     * Interpolate between two arrays of different size.
     */
    private static void DetermineDest() {
        int XInc, YInc;
        int XSizeMax = Data.IRData_NumberColumns - 1;
        int YSizeMax = Data.IRData_NumberRows - 1;

        double NWCornerLatitude = Data.IRData_Latitude[0][0];
        double NWCornerLongitude = Data.IRData_Longitude[0][0];
        double NECornerLatitude = Data.IRData_Latitude[0][XSizeMax];
        double NECornerLongitude = Data.IRData_Longitude[0][XSizeMax];
        double SWCornerLatitude = Data.IRData_Latitude[YSizeMax][0];
        double SWCornerLongitude = Data.IRData_Longitude[YSizeMax][0];
        double SECornerLatitude = Data.IRData_Latitude[YSizeMax][XSizeMax];
        double SECornerLongitude = Data.IRData_Longitude[YSizeMax][XSizeMax];

        /* crosses dateline check */
        if ((NWCornerLongitude < NECornerLongitude) || (SWCornerLongitude < SECornerLongitude)) {
            NWCornerLongitude = NWCornerLongitude + 360.0;
            if (SWCornerLongitude < SECornerLongitude) {
                SWCornerLongitude = SWCornerLongitude + 360.0;
            }
            /* System.out.printf("DATELINE CROSS\n"); */
            for (XInc = 0; XInc < XSizeMax; XInc++) {
                for (YInc = 0; YInc < YSizeMax; YInc++) {
                    double DataValue = Data.IRData_Longitude[YInc][XInc];
                    if (DataValue < 0.0) {
                        Data.IRData_Longitude[YInc][XInc] = (float) (DataValue + 360.0);
                    }
                }
            }
            NWCornerLatitude = Data.IRData_Latitude[0][0];
            NWCornerLongitude = Data.IRData_Longitude[0][0];
            NECornerLatitude = Data.IRData_Latitude[0][XSizeMax];
            NECornerLongitude = Data.IRData_Longitude[0][XSizeMax];
            SWCornerLatitude = Data.IRData_Latitude[YSizeMax][0];
            SWCornerLongitude = Data.IRData_Longitude[YSizeMax][0];
            SECornerLatitude = Data.IRData_Latitude[YSizeMax][XSizeMax];
            SECornerLongitude = Data.IRData_Longitude[YSizeMax][XSizeMax];
        }

        double MaximumLatitudeValue = Math.min(NWCornerLatitude, NECornerLatitude);
        double MinimumLatitudeValue = Math.max(SWCornerLatitude, SECornerLatitude);
        double MaximumLongitudeValue = Math.max(NWCornerLongitude, SWCornerLongitude);
        double MinimumLongitudeValue = Math.min(NECornerLongitude, SECornerLongitude);

        double LatitudeIncrement = (MaximumLatitudeValue - MinimumLatitudeValue)
                / (double) Data.IRData_NumberColumns;
        double LongitudeIncrement = (MaximumLongitudeValue - MinimumLongitudeValue)
                / (double) Data.IRData_NumberRows;
        double MaximumIncrementValue = Math.max(LatitudeIncrement, LongitudeIncrement);
        System.out.printf("REMAPPING INFO\n");
        System.out.printf("Source Array Bounds\n");
        System.out.printf("  NW Corner : %7.2f/%7.2f\n", NWCornerLatitude, NWCornerLongitude);
        System.out.printf("  NE Corner : %7.2f/%7.2f\n", NECornerLatitude, NECornerLongitude);
        System.out.printf("  SW Corner : %7.2f/%7.2f\n", SWCornerLatitude, SWCornerLongitude);
        System.out.printf("  SE Corner : %7.2f/%7.2f\n", SECornerLatitude, SECornerLongitude);
        System.out.printf("Destination Array Bounds\n");
        System.out.printf("  Max Lat/Lon: %7.2f/%7.2f\n", MaximumLatitudeValue,
                MaximumLongitudeValue);
        System.out.printf("  Min Lat/Lon: %7.2f/%7.2f\n", MinimumLatitudeValue,
                MinimumLongitudeValue);
        System.out.printf("  Inc Lat/Lon:   %5.3f/  %5.3f\n", MaximumIncrementValue,
                MaximumIncrementValue);

        tiff_vars.out_lines = (int) ((MaximumLatitudeValue - MinimumLatitudeValue) / MaximumIncrementValue);
        tiff_vars.out_elems = (int) ((MaximumLongitudeValue - MinimumLongitudeValue) / MaximumIncrementValue);
        for (YInc = 0; YInc < tiff_vars.out_lines; YInc++) {
            double LatitudeValue = MaximumLatitudeValue - (YInc * MaximumIncrementValue);
            for (XInc = 0; XInc < tiff_vars.out_elems; XInc++) {
                double LongitudeValue = MaximumLongitudeValue - (XInc * MaximumIncrementValue);
                IRData_Remap_Latitude[YInc][XInc] = LatitudeValue;
                IRData_Remap_Longitude[YInc][XInc] = LongitudeValue;
            }
        }

        IRData_Remap_NumberColumns = tiff_vars.out_elems;
        IRData_Remap_NumberRows = tiff_vars.out_lines;
    }

    /**
     * Compute number of corners for transformation and block sizes.
     *
     * @param LineSplineInput
     *            Spline function for line values.
     * @param ElementSplineInput
     *            Spline function for element values.
     *
     * @return Total number of corners to interpolate.
     */
    private static int Init(int LineSplineInput, int ElementSplineInput) {
        int NumberOfCorners = 0;

        remap_vars.nspl = (tiff_vars.out_elems + ElementSplineInput - 1) / ElementSplineInput;
        remap_vars.nspe = (tiff_vars.out_lines + LineSplineInput - 1) / LineSplineInput;

        remap_vars.ncl = remap_vars.nspl + 1;
        remap_vars.nce = remap_vars.nspe + 1;

        if (((tiff_vars.out_elems + ElementSplineInput - 1) % ElementSplineInput) == 0) {
            remap_vars.ncl = remap_vars.nspl;
        }
        if (((tiff_vars.out_lines + LineSplineInput - 1) % LineSplineInput) == 0) {
            remap_vars.nce = remap_vars.nspe;
        }

        NumberOfCorners = remap_vars.ncl * remap_vars.nce;

        remap_vars.in_bfw = Math.max(MINBFW, MINBLKSIZ * tiff_vars.in_elems);
        remap_vars.out_bfw = Math.max(MINBFW, Math.max(LineSplineInput, MINBLKSIZ)
                * tiff_vars.out_elems);

        remap_vars.slb = remap_vars.in_bfw / tiff_vars.in_elems;
        remap_vars.dlb = ((remap_vars.out_bfw / tiff_vars.out_elems) / LineSplineInput)
                * LineSplineInput;

        return NumberOfCorners;
    }

    /**
     * Compute transformations at corners.
     *
     * Operates on the {@link #LineCoordinateArray} and
     * {@link #ElementCoordinateArray}.
     *
     * @param NumberOfCornersInput
     *            Total number of corners to interpolate.
     * @param LineSplineInput
     *            Spline function for line values.
     * @param ElementSplineInput
     *            Spline function for element values.
     */
    private static void Corner(int NumberOfCornersInput, int LineSplineInput, int ElementSplineInput) {
        int XInc;
        int YInc;
        int ArrayInc;

        /* initialize array of corners */
        for (ArrayInc = 0; ArrayInc < NumberOfCornersInput; ArrayInc++) {
            LineCoordinateArray[ArrayInc] = -99.9;
            ElementCoordinateArray[ArrayInc] = -99.9;
        }

        /* loop through destination file and record source coords */
        ArrayInc = -1;
        int NumberLines = tiff_vars.out_lines + LineSplineInput - 1;
        int NumberElements = tiff_vars.out_elems + ElementSplineInput - 1;

        for (YInc = 0; YInc < NumberLines; YInc = YInc + LineSplineInput) {
            int LineValue = YInc;
            for (XInc = 0; XInc < NumberElements; XInc = XInc + ElementSplineInput) {
                /* System.out.printf("yinc=%d xinc=%d... \n",YInc,XInc); */
                int ElementValue = XInc;
                int[] UMapReturn = UMap(LineValue, ElementValue);
                int RetErr = UMapReturn[0];
                int SplineLineValue = UMapReturn[1];
                int SplineElementValue = UMapReturn[2];
                /*
                 * System.out.printf("spline line=%d element=%d \n ",SplineLineValue
                 * ,SplineElementValue);
                 */
                if ((ElementSplineInput == 1) && (LineSplineInput == 1)) {
                    IRData_Remap_Temperature[LineValue][ElementValue] = Data.IRData_Temperature[SplineLineValue][SplineElementValue];
                } else {
                    ++ArrayInc;
                    if (RetErr == 0) {
                        LineCoordinateArray[ArrayInc] = SplineLineValue;
                        ElementCoordinateArray[ArrayInc] = SplineElementValue;
                    }
                }
            }
        }

    }

    /**
     * Provide coordinates between original point and transformed point.
     *
     * @param LineValueInput
     *            Original line coordinate.
     * @param ElementValueInput
     *            Ooriginal element coordinate.
     *
     * @return Array containing three values: possible error code, interpolated
     *         line coordinate, and interpolated element coordinate.
     */
    private static int[] UMap(int LineValueInput, int ElementValueInput) {

        /* dest array line position value */
        double DestinationLatitude;

        /* dest array element position value */
        double DestinationLongitude;

        /*
         * Convert destination LineValueInput, ElementValueInput to source
         * coords LineValue_Return, ElementValue_Return
         */
        LineValueInput = Math.min(LineValueInput, tiff_vars.out_lines - 1);
        ElementValueInput = Math.min(ElementValueInput, tiff_vars.out_elems - 1);
        DestinationLatitude = IRData_Remap_Latitude[LineValueInput][ElementValueInput];
        DestinationLongitude = IRData_Remap_Longitude[LineValueInput][ElementValueInput];

        int[] FindPointReturn = FindPoint(DestinationLatitude, DestinationLongitude);
        int RetErr = FindPointReturn[0];
        int LineValueReturn = FindPointReturn[1];
        int ElementValueReturn = FindPointReturn[2];

        return new int[] { RetErr, LineValueReturn, ElementValueReturn };
    }

    /**
     * Find specific lat/lon location in array and return index values.
     *
     * @param latitude
     *            Latitude value.
     * @param longitude
     *            Longitude value.
     *
     * @return Array of three values. The first value is the status (-1 for
     *         error, 0 for ok), the second value is array line value of lat/lon
     *         input, and the third value is array element value of lat/lon
     *         input.
     */
    private static int[] FindPoint(double latitude, double longitude) {
        int RetErr;
        int XInc;
        int IndexValue;
        int YLineValueReturn;
        int XElementValueReturn;

        double[] CornerLatitudeArray = new double[4];
        double[] CornerLongitudeArray = new double[4];
        double[] NSEWDistanceValuesArray = new double[4];

        /* found point logical */
        boolean FoundPointTF = false;

        /* out of bounds flag logical */
        boolean OutOfBoundsTF = false;

        /* found latitude value logical */
        boolean FoundLatitudeTF = false;

        /* found longitude value logical */
        boolean FoundLongitudeTF = false;

        int NumberElements = tiff_vars.in_elems;
        int NumberLines = tiff_vars.in_lines;
        int ElementValue = 0;
        int LineValue = 0;
        double PreviousDistance = 9999.9;

        for (XInc = 0; XInc < 4; XInc++) {
            NSEWDistanceValuesArray[XInc] = 0.0;
            CornerLatitudeArray[XInc] = 0.0;
            CornerLongitudeArray[XInc] = 0.0;
        }
        while ((!FoundPointTF) && (!OutOfBoundsTF)) {
            CornerLatitudeArray[0] = Data.IRData_Latitude[LineValue][ElementValue];
            CornerLongitudeArray[0] = Data.IRData_Longitude[LineValue][ElementValue];
            CornerLatitudeArray[3] = Data.IRData_Latitude[LineValue + 1][ElementValue + 1];
            CornerLongitudeArray[3] = Data.IRData_Longitude[LineValue + 1][ElementValue + 1];
            /*
             * System.out.printf(
             * "x=%d  y=%d  : CornerLatitudeArray0=%f CornerLongitudeArray0=%f "
             * , " CornerLatitudeArray3=%f CornerLongitudeArray3=%f\n",
             * ElementValue,LineValue,
             * CornerLatitudeArray[0],CornerLongitudeArray[0],
             * CornerLatitudeArray[3],CornerLongitudeArray[3]);
             */
            if ((longitude > CornerLongitudeArray[0]) || (longitude < CornerLongitudeArray[3])) {
                FoundLongitudeTF = false;
                if (longitude < CornerLongitudeArray[3]) {
                    ElementValue++;
                } else {
                    if (longitude > CornerLongitudeArray[0]) {
                        ElementValue--;
                    }
                }
            } else {
                FoundLongitudeTF = true;
            }
            if ((latitude > CornerLatitudeArray[0]) || (latitude < CornerLatitudeArray[3])) {
                FoundLatitudeTF = false;
                if (latitude < CornerLatitudeArray[3]) {
                    LineValue++;
                } else {
                    if (latitude > CornerLatitudeArray[0]) {
                        LineValue--;
                    }
                }
            } else {
                FoundLatitudeTF = true;
            }
            double LocalValue1[] = Functions.distance_angle(latitude, longitude,
                    CornerLatitudeArray[0], CornerLongitudeArray[0], 1);
            double DistanceValue = LocalValue1[0];

            /*
             * * System.out.printf("distance : latitude=%f longitude=%f",*
             * " CornerLatitudeArray0=%f CornerLongitudeArray0=%f",*
             * " dist=%f angle=%f\n",latitude,longitude,*
             * CornerLatitudeArray[0],CornerLongitudeArray[0],*
             * DistanceValue,AngleValue);
             */
            if (FoundLatitudeTF && FoundLongitudeTF) {
                FoundPointTF = true;
            }
            if (PreviousDistance <= DistanceValue) {
                FoundPointTF = true;
            }
            if ((ElementValue < 0) || (ElementValue > (NumberElements - 2))) {
                OutOfBoundsTF = true;
            }
            if ((LineValue < 0) || (LineValue > (NumberLines - 2))) {
                OutOfBoundsTF = true;
            }
            PreviousDistance = DistanceValue;
        }
        if (FoundPointTF) {
            CornerLatitudeArray[1] = Data.IRData_Latitude[LineValue][ElementValue + 1];
            CornerLongitudeArray[1] = Data.IRData_Longitude[LineValue][ElementValue + 1];
            CornerLatitudeArray[2] = Data.IRData_Latitude[LineValue + 1][ElementValue];
            CornerLongitudeArray[2] = Data.IRData_Longitude[LineValue + 1][ElementValue];

            double LocalValue2[] = Functions.distance_angle(latitude, longitude,
                    CornerLatitudeArray[0], CornerLongitudeArray[0], 1);
            NSEWDistanceValuesArray[0] = LocalValue2[0];
            double LocalValue3[] = Functions.distance_angle(latitude, longitude,
                    CornerLatitudeArray[1], CornerLongitudeArray[1], 1);
            NSEWDistanceValuesArray[1] = LocalValue3[0];
            IndexValue = (NSEWDistanceValuesArray[0] < NSEWDistanceValuesArray[1]) ? 0 : 1;
            double LocalValue4[] = Functions.distance_angle(latitude, longitude,
                    CornerLatitudeArray[2], CornerLongitudeArray[2], 1);
            NSEWDistanceValuesArray[2] = LocalValue4[0];
            IndexValue = (NSEWDistanceValuesArray[IndexValue] < NSEWDistanceValuesArray[2]) ? IndexValue
                    : 2;
            double LocalValue5[] = Functions.distance_angle(latitude, longitude,
                    CornerLatitudeArray[3], CornerLongitudeArray[3], 1);
            NSEWDistanceValuesArray[3] = LocalValue5[0];
            IndexValue = (NSEWDistanceValuesArray[IndexValue] < NSEWDistanceValuesArray[3]) ? IndexValue
                    : 3;

            XElementValueReturn = ((IndexValue == 0) || (IndexValue == 2)) ? ElementValue
                    : ElementValue + 1;
            YLineValueReturn = ((IndexValue == 0) || (IndexValue == 1)) ? LineValue : LineValue + 1;
            RetErr = 0;
        } else {
            XElementValueReturn = -1;
            YLineValueReturn = -1;
            RetErr = -1;
        }

        return new int[] { RetErr, YLineValueReturn, XElementValueReturn };
    }

    private static int DoMap(int NumberOfCornersInput, int LineSplineInput, int ElementSplineInput) {
        /* LineCoordsArrayInput = LineCoordinateArray */
        /* ElementCoordsArrayInput = ElementCoordinateArray */

        /* line increment */
        int[] LineIncArray1 = { -2, -2, 0, 2, 2, 2, 0, -2 };

        /* line increment */
        int[] LineIncArray2 = { -1, -1, 0, 1, 1, 1, 0, -1 };

        /* ele increment */
        int[] ElementIncArray1 = { 0, 2, 2, 2, 0, -2, -2, -2 };

        /* ele increment */
        int[] ElementIncArray2 = { 0, 1, 1, 1, 0, -1, -1, -1 };

        int BufferSize = tiff_vars.in_lines * tiff_vars.in_elems;

        double[] SourceArray = new double[BufferSize];
        double[] InterpArray = new double[BufferSize];
        double[] TempCharArray = new double[NumberOfCornersInput];

        int Ret_ID;
        int XInc, YInc, IncVal;
        int ArrayIndexValue;
        int ArrayPointValue;
        int SplinePixelValue;
        int SourceBlockValue;
        int SourceBlockInc;
        int SplineInc;
        int OffsetValue;
        int OffsetValue0;
        int OffsetValue1;
        int OffsetValue2;
        int OffsetValueL = 0;
        int OffsetValueE = 0;
        int CornerPointer;
        int AccumulatedLines;
        int FirstCornerPointer;
        int FirstCornerPointerHold;
        int LastCornerPointer;
        int ValX;
        int LineMinimumFinal;
        int LineMaximumFinal;
        int ElementMinimumFinal;
        int ElementMaximumFinal;
        int EdgeFixID;
        int SourceLine = 0;
        int SourceElement = 0;
        int SourceOffsetValue;
        int MaximumSourceLine;

        double LineValueA;
        double LineValueB;
        double LineValueC;
        double ElementValueA;
        double ElementValueB;
        double ElementValueC;
        double LineValueACounter;
        double LineValueBCounter;
        double LineValueCCounter;
        double ElementValueACounter;
        double ElementValueBCounter;
        double ElementValueCCounter;
        double LineValueACounter0;
        double ElementValueACounter0;
        double LineULValue;
        double LineURValue;
        double LineLLValue;
        double LineLRValue;
        double LineMinimum;
        double LineMaximum;
        double ElementULValue;
        double ElementURValue;
        double ElementLLValue;
        double ElementLRValue;
        double ElementMinimum;
        double ElementMaximum;
        boolean ReadAllTF = true;
        boolean SKIP;
        boolean LABEL30;
        boolean LABEL38;

        if ((SourceArray == null) || (InterpArray == null) || (TempCharArray == null)) {
            Ret_ID = -1;
        } else {
            SplinePixelValue = LineSplineInput * ElementSplineInput;

            for (YInc = 0; YInc < tiff_vars.in_lines; YInc++) {
                for (XInc = 0; XInc < tiff_vars.in_elems; XInc++) {
                    ArrayIndexValue = (YInc * tiff_vars.in_elems) + XInc;
                    SourceArray[ArrayIndexValue] = Data.IRData_Temperature[YInc][XInc];
                }
            }

            /*
             * System.out.printf("remap nce=%d ncl=%d\n",remap_vars.nce,remap_vars
             * .ncl);
             */
            for (YInc = 1; YInc < remap_vars.nce + 1; YInc++) {
                for (XInc = 1; XInc < remap_vars.ncl + 1; XInc++) {
                    ArrayPointValue = IND(YInc, XInc);

                    if (LineCoordinateArray[ArrayPointValue - 1] == (double) -99.9) {
                        double LineSum = 0.0;
                        double ElementSum = 0.0;
                        int PointCounter = 0;
                        for (IncVal = 0; IncVal < 8; IncVal++) {
                            SKIP = false;
                            int YInc1 = YInc + LineIncArray1[IncVal];
                            int YInc2 = YInc + LineIncArray2[IncVal];
                            if ((YInc1 < 1) || (YInc2 < 1)) {
                                SKIP = true;
                            }
                            if ((YInc1 > remap_vars.nce) || (YInc2 > remap_vars.nce)) {
                                SKIP = true;
                            }

                            int XInc1 = XInc + ElementIncArray1[IncVal];
                            int XInc2 = XInc + ElementIncArray2[IncVal];

                            if ((XInc1 < 1) || (XInc2 < 1)) {
                                SKIP = true;
                            }
                            if ((XInc1 > remap_vars.ncl) || (XInc2 > remap_vars.ncl)) {
                                SKIP = true;
                            }

                            int ValueIND1 = IND(YInc1, XInc1);
                            int ValueIND2 = IND(YInc2, XInc2);

                            if (!SKIP) {
                                if ((LineCoordinateArray[ValueIND1 - 1] == -99.9)
                                        || (LineCoordinateArray[ValueIND2 - 1] == -99.9)) {
                                    SKIP = true;
                                }
                                if (TempCharArray[ValueIND1 - 1] != 0) {
                                    SKIP = true;
                                }
                                if (TempCharArray[ValueIND2 - 1] != 0) {
                                    SKIP = true;
                                }
                                if (!SKIP) {
                                    PointCounter = PointCounter + 1;
                                    LineSum = LineSum
                                            + ((double) (2.0 * LineCoordinateArray[ValueIND2 - 1]))
                                            - LineCoordinateArray[ValueIND1 - 1];
                                    ElementSum = ElementSum
                                            + ((double) 2.0 * ElementCoordinateArray[ValueIND2 - 1])
                                            - ElementCoordinateArray[ValueIND1 - 1];
                                } /* SKIP:; */
                            } /* SKIP:; */
                        }

                        if (PointCounter > 0) {
                            LineCoordinateArray[ArrayPointValue - 1] = LineSum / PointCounter;
                            ElementCoordinateArray[ArrayPointValue - 1] = ElementSum / PointCounter;
                            TempCharArray[ArrayPointValue - 1] = 1;
                        }
                    }
                }
            }

            /* Loop through by destination blocks */
            int BlockCounter = 0;

            for (IncVal = 1; IncVal < tiff_vars.out_lines + 1; IncVal = IncVal + remap_vars.dlb) {
                /* Accumulated lines/block */
                AccumulatedLines = BlockCounter * remap_vars.dlb;

                /* Pointer to first corner of splines for this dest block */
                FirstCornerPointer = (BlockCounter * remap_vars.ncl * remap_vars.dlb)
                        / LineSplineInput;
                FirstCornerPointerHold = FirstCornerPointer;

                /* Pointer to last corner for this dest block */
                LastCornerPointer = (((BlockCounter + 1) * remap_vars.ncl * remap_vars.dlb) / LineSplineInput) - 1;
                LastCornerPointer = Math.min(LastCornerPointer, NumberOfCornersInput
                        - remap_vars.ncl);

                /* For each destination block loop through entire source */
                for (SourceBlockInc = 1; SourceBlockInc < tiff_vars.in_lines + 1; SourceBlockInc = SourceBlockInc
                        + remap_vars.slb) {
                    MaximumSourceLine = Math.min(tiff_vars.in_lines, (SourceBlockInc
                            + remap_vars.slb - 1));
                    ReadAllTF = false;

                    /* Loop through splines and move any data */

                    FirstCornerPointer = FirstCornerPointerHold;
                    SourceBlockValue = 0;
                    while (FirstCornerPointer < LastCornerPointer) {
                        for (SplineInc = 1; SplineInc < remap_vars.nspl + 1; SplineInc++) {
                            LABEL30 = false;
                            OffsetValue0 = (SourceBlockValue / remap_vars.nspl) * LineSplineInput
                                    * tiff_vars.out_elems;
                            OffsetValue1 = (SourceBlockValue % remap_vars.nspl)
                                    * ElementSplineInput;
                            OffsetValue2 = OffsetValue1 + OffsetValue0 + 1;
                            CornerPointer = FirstCornerPointer + SplineInc - 1;

                            /*
                             * Get 4 corners in line space and check for out of
                             * bounds
                             */

                            ValX = remap_vars.ncl;
                            LineULValue = LineCoordinateArray[CornerPointer];
                            LineURValue = LineCoordinateArray[CornerPointer + 1];
                            LineLLValue = LineCoordinateArray[CornerPointer + ValX];
                            if ((CornerPointer + 1 + ValX) < NumberOfCornersInput) {
                                LineLRValue = LineCoordinateArray[CornerPointer + 1 + ValX];
                            } else {
                                LineLRValue = LineCoordinateArray[CornerPointer + ValX];
                            }
                            LineMinimum = Math.min(LineULValue, LineURValue);
                            LineMinimum = Math.min(LineMinimum, LineLLValue);
                            LineMinimum = Math.min(LineMinimum, LineLRValue);

                            /* Test for the presence of a limb in the spline box */

                            if (LineMinimum == -99.9) {
                                LABEL30 = true;
                            }

                            LineMinimumFinal = (int) (LineMinimum + 0.5);
                            if (LineMinimumFinal > MaximumSourceLine) {
                                LABEL30 = true;
                            }
                            LineMaximum = Math.max(LineULValue, LineURValue);
                            LineMaximum = Math.max(LineMaximum, LineLLValue);
                            LineMaximum = Math.max(LineMaximum, LineLRValue);
                            LineMaximumFinal = (int) (LineMaximum + 0.5);
                            if (LineMaximumFinal < SourceBlockInc) {
                                LABEL30 = true;
                            }

                            /*
                             * Get 4 corners in elem space & check for out of
                             * bound
                             */

                            ValX = remap_vars.ncl;
                            ElementULValue = ElementCoordinateArray[CornerPointer];
                            ElementURValue = ElementCoordinateArray[CornerPointer + 1];
                            ElementLLValue = ElementCoordinateArray[CornerPointer + ValX];
                            if ((CornerPointer + 1 + ValX) < NumberOfCornersInput) {
                                ElementLRValue = ElementCoordinateArray[CornerPointer + 1 + ValX];
                            } else {
                                ElementLRValue = ElementCoordinateArray[CornerPointer + ValX];
                            }

                            ElementMaximum = Math.max(ElementULValue, ElementURValue);
                            ElementMaximum = Math.max(ElementMaximum, ElementLLValue);
                            ElementMaximum = Math.max(ElementMaximum, ElementLRValue);
                            ElementMaximumFinal = (int) (ElementMaximum + 0.5);
                            if (ElementMaximumFinal < 1) {
                                LABEL30 = true;
                            }

                            ElementMinimum = Math.min(ElementULValue, ElementURValue);
                            ElementMinimum = Math.min(ElementMinimum, ElementLLValue);
                            ElementMinimum = Math.min(ElementMinimum, ElementLRValue);
                            ElementMinimumFinal = (int) (ElementMinimum + 0.5);

                            if (ElementMinimumFinal > tiff_vars.in_elems) {
                                LABEL30 = true;
                            }
                            EdgeFixID = 0;

                            /*
                             * If the max & min element fall off the
                             * image...pitch it
                             */

                            if ((ElementMaximumFinal > tiff_vars.in_elems)
                                    && (ElementMinimumFinal < 1)) {
                                LABEL30 = true;
                            }

                            /* Fix if left & right edge should be continuous */

                            if (!LABEL30) {
                                if ((ElementMaximumFinal - ElementMinimumFinal) > (int) (.75 * tiff_vars.in_elems)) {
                                    if (ElementULValue < (tiff_vars.in_elems / 2)) {
                                        ElementULValue = ElementULValue + tiff_vars.in_elems;
                                    }
                                    if (ElementURValue < (tiff_vars.in_elems / 2)) {
                                        ElementURValue = ElementURValue + tiff_vars.in_elems;
                                    }
                                    if (ElementLLValue < (tiff_vars.in_elems / 2)) {
                                        ElementLLValue = ElementLLValue + tiff_vars.in_elems;
                                    }
                                    if (ElementLRValue < (tiff_vars.in_elems / 2)) {
                                        ElementLRValue = ElementLRValue + tiff_vars.in_elems;
                                    }
                                    EdgeFixID = 1;
                                }

                                LineValueA = (LineURValue - LineULValue) / ElementSplineInput;
                                LineValueB = (LineLLValue - LineULValue) / LineSplineInput;
                                LineValueC = (LineLRValue + LineULValue - LineURValue - LineLLValue)
                                        / SplinePixelValue;
                                ElementValueA = (ElementURValue - ElementULValue)
                                        / ElementSplineInput;
                                ElementValueB = (ElementLLValue - ElementULValue) / LineSplineInput;
                                ElementValueC = (ElementLRValue + ElementULValue - ElementLLValue - ElementURValue)
                                        / SplinePixelValue;

                                int ElementMiscValue = 0;
                                LineValueBCounter = LineULValue + 0.5;
                                LineValueCCounter = 0.0;
                                ElementValueBCounter = ElementULValue + 0.5;
                                ElementValueCCounter = 0.0;

                                if (ReadAllTF == false) {
                                    ReadAllTF = true;
                                }

                                if ((SplineInc == remap_vars.nspl)
                                        || (ElementMinimumFinal < 1 || (ElementMaximumFinal > tiff_vars.in_elems))
                                        || ((LineMinimumFinal < SourceBlockInc) || (LineMaximumFinal > MaximumSourceLine))
                                        || ((FirstCornerPointer + (2 * remap_vars.ncl) - 1) > LastCornerPointer)) {
                                    for (YInc = 1; YInc < LineSplineInput + 1; YInc++) {
                                        LineValueACounter = LineValueCCounter + LineValueA;
                                        ElementValueACounter = ElementValueCCounter + ElementValueA;
                                        LineValueACounter0 = 0.0;
                                        ElementValueACounter0 = 0.0;
                                        for (XInc = 1; XInc < ElementSplineInput + 1; XInc++) {
                                            LABEL38 = false;
                                            SourceLine = (int) (LineValueBCounter + LineValueACounter0);
                                            if (SourceLine < SourceBlockInc) {
                                                LABEL38 = true;
                                            }
                                            if (SourceLine > MaximumSourceLine) {
                                                LABEL38 = true;
                                            }
                                            if (!LABEL38)
                                                SourceElement = (int) (ElementValueBCounter + ElementValueACounter0);
                                            if (SourceElement < 1) {
                                                LABEL38 = true;
                                            }
                                            if ((SourceElement > tiff_vars.in_elems)
                                                    && (EdgeFixID == 0)) {
                                                LABEL38 = true;
                                            }
                                            if (!LABEL38) {
                                                if (SourceElement > tiff_vars.in_elems) {
                                                    SourceElement = SourceElement
                                                            - tiff_vars.in_elems;
                                                }
                                                SourceOffsetValue = ((SourceLine - SourceBlockInc) * tiff_vars.in_elems)
                                                        + SourceElement;
                                                OffsetValueE = OffsetValue1 + XInc;
                                                if (OffsetValueE > tiff_vars.out_elems) {
                                                    LABEL38 = true;
                                                }
                                                if (!LABEL38)
                                                    OffsetValueL = OffsetValue0 + ElementMiscValue;
                                                if ((OffsetValueL / (tiff_vars.out_elems
                                                        + AccumulatedLines - 1)) > tiff_vars.out_lines) {
                                                    LABEL38 = true;
                                                }
                                                if (!LABEL38) {
                                                    OffsetValue = OffsetValueL + OffsetValueE;
                                                    InterpArray[OffsetValue - 1] = SourceArray[SourceOffsetValue - 1];
                                                } /* LABEL38: */
                                            } /* LABEL38: */
                                            LineValueACounter0 = LineValueACounter0
                                                    + LineValueACounter;
                                            ElementValueACounter0 = ElementValueACounter0
                                                    + ElementValueACounter;
                                        }
                                        LineValueBCounter = LineValueBCounter + LineValueB;
                                        LineValueCCounter = LineValueCCounter + LineValueC;
                                        ElementValueBCounter = ElementValueBCounter + ElementValueB;
                                        ElementValueCCounter = ElementValueCCounter + ElementValueC;
                                        ElementMiscValue = ElementMiscValue + tiff_vars.out_elems;
                                    }
                                } else {
                                    if (EdgeFixID == 0) {
                                        for (YInc = 1; YInc < LineSplineInput + 1; YInc++) {
                                            LineValueACounter = LineValueCCounter + LineValueA;
                                            ElementValueACounter = ElementValueCCounter
                                                    + ElementValueA;
                                            LineValueACounter0 = 0.0;
                                            ElementValueACounter0 = 0.0;
                                            OffsetValue = OffsetValue2 + ElementMiscValue;
                                            for (XInc = 1; XInc < ElementSplineInput + 1; XInc++) {
                                                SourceLine = (int) (LineValueBCounter + LineValueACounter0);
                                                SourceElement = (int) (ElementValueBCounter + ElementValueACounter0);
                                                SourceOffsetValue = ((SourceLine - SourceBlockInc) * tiff_vars.in_elems)
                                                        + SourceElement;
                                                InterpArray[OffsetValue - 1] = SourceArray[SourceOffsetValue - 1];
                                                OffsetValue = OffsetValue + 1;
                                                LineValueACounter0 = LineValueACounter0
                                                        + LineValueACounter;
                                                ElementValueACounter0 = ElementValueACounter0
                                                        + ElementValueACounter;
                                            }
                                            LineValueBCounter = LineValueBCounter + LineValueB;
                                            LineValueCCounter = LineValueCCounter + LineValueC;
                                            ElementValueBCounter = ElementValueBCounter
                                                    + ElementValueB;
                                            ElementValueCCounter = ElementValueCCounter
                                                    + ElementValueC;
                                            ElementMiscValue = ElementMiscValue
                                                    + tiff_vars.out_elems;
                                        }
                                    } else if (EdgeFixID == 1) {
                                        for (YInc = 1; YInc < LineSplineInput + 1; YInc++) {
                                            LineValueACounter = LineValueCCounter + LineValueA;
                                            ElementValueACounter = ElementValueCCounter
                                                    + ElementValueA;
                                            LineValueACounter0 = 0.0;
                                            ElementValueACounter0 = 0.0;
                                            OffsetValue = OffsetValue2 + ElementMiscValue;
                                            for (XInc = 1; XInc < ElementSplineInput + 1; XInc++) {
                                                SourceLine = (int) (LineValueBCounter + LineValueACounter0);
                                                SourceElement = (int) (ElementValueBCounter + ElementValueACounter0);
                                                if (SourceElement > tiff_vars.in_elems) {
                                                    SourceElement = SourceElement
                                                            - tiff_vars.in_elems;
                                                }
                                                SourceOffsetValue = ((SourceLine - SourceBlockInc) * tiff_vars.in_elems)
                                                        + SourceElement;
                                                InterpArray[OffsetValue - 1] = SourceArray[SourceOffsetValue - 1];
                                                OffsetValue = OffsetValue + 1;
                                                LineValueACounter0 = LineValueACounter0
                                                        + LineValueACounter;
                                                ElementValueACounter0 = ElementValueACounter0
                                                        + ElementValueACounter;
                                            }
                                            LineValueBCounter = LineValueBCounter + LineValueB;
                                            LineValueCCounter = LineValueCCounter + LineValueC;
                                            ElementValueBCounter = ElementValueBCounter
                                                    + ElementValueB;
                                            ElementValueCCounter = ElementValueCCounter
                                                    + ElementValueC;
                                            ElementMiscValue = ElementMiscValue
                                                    + tiff_vars.out_elems;
                                        }
                                    }
                                }
                            } /* LABEL30: */
                            SourceBlockValue = SourceBlockValue + 1;
                        }
                        FirstCornerPointer = FirstCornerPointer + remap_vars.ncl;
                    }
                }
                BlockCounter = BlockCounter + 1;
            }

            for (YInc = 0; YInc < tiff_vars.out_lines; YInc++) {
                for (XInc = 0; XInc < tiff_vars.out_elems; XInc++) {
                    ArrayIndexValue = (YInc * tiff_vars.out_elems) + XInc;
                    if ((ArrayIndexValue > 0) && (InterpArray[ArrayIndexValue] == 0.0)) {
                        InterpArray[ArrayIndexValue] = InterpArray[ArrayIndexValue - 1];
                    }
                    IRData_Remap_Temperature[YInc][XInc] = InterpArray[ArrayIndexValue];
                }
            }
            Ret_ID = 0;
        }

        return Ret_ID;

    }

    private static int IND(int y, int x) {
        /* #define IND(y,x) ((y-1)*remap_vars.ncl+x) */
        return ((y - 1) * remap_vars.ncl + x);
    }

    /**
     * Determine storm position at time CurrentTime using NHC/JTWC forecast
     * discussion products.
     *
     * Time and location information from these products are then interpolated
     * to time in question to derive a estimated storm position. If position
     * estimation cannot be calculated, a lat/lon position of -99.5/-999.5 will
     * be returned. Inputs : None Outputs : Latitude_Return - estimated latitude
     * position Longitude_Return - estimated longitude position
     * PositioningMethodID_Return - method used to derive storm location Return
     * : -43 : Error w/ forecast file open and BAD extrapolation -44 : Invalid
     * forecast file and BAD extrapolation -45 : Error w/ forecast file read and
     * BAD extrapolation -46 : Error w/ forecast interpolation and BAD
     * extrapolation 42 : GOOD INTERPOLATION 43 : Error w/ forecast file open
     * but GOOD EXTRAPOLATION 44 : Invalid forecast file but GOOD extrapolation
     * 45 : Error w/ forecast file read but GOOD extrapolation 46 : Error w/
     * forecast interpolation but GOOD EXTRAPOLATION 0 : Subroutine Error
     */
    public double[] AutoMode1(String ForecastFile, int ForecastFileType) throws IOException {

        int RetID = 0;
        int PositioningMethodID = 0;
        int InterpolatedReturnFlag = 0;
        double InterpolatedLatitude = -99.99;
        double InterpolatedLongitude = -99.99;
        double InterpolatedIntensity = -99.99;
        boolean UseExtrapPositionTF = false;

        /* Read Forecast File */
        double ThresholdTime = 24.0;
        double[] ForecastFileOutput = Forecasts.ReadForecasts(ForecastFile, ForecastFileType,
                ThresholdTime);

        if (ForecastFileOutput == null) {
            JOptionPane.showMessageDialog(null,
                    "Unable to process Forecast File, please ensure a valid file is present.");
            return null;
        }

        InterpolatedReturnFlag = (int) ForecastFileOutput[0];
        InterpolatedLatitude = ForecastFileOutput[1];
        InterpolatedLongitude = ForecastFileOutput[2];
        InterpolatedIntensity = ForecastFileOutput[3];
        /* History.IRCurrentRecord.latitude = ForecastLatitude; */
        /* History.IRCurrentRecord.longitude = ForecastLongitude; */

        /*
         * System.out.printf("InterpolatedReturnFlag=%d\n",InterpolatedReturnFlag
         * );
         */
        if (InterpolatedReturnFlag != 0) {
            PositioningMethodID = 6;
            UseExtrapPositionTF = true;
            /*
             * -1 will give 45=invalid interpretation -2 will give 44=invalid
             * file type -4 will give 42=error closing file -5 will give
             * 41=error opening file
             */
            RetID = 46 + InterpolatedReturnFlag;
        } else {
            /* Good interpolated values... use 'em */
            PositioningMethodID = 1;
            UseExtrapPositionTF = false;
            RetID = 43;
        }

        /*
         * try to extrapolate storm location from previous storm locations in
         * history file
         */
        if (UseExtrapPositionTF) {
            /* call slopecal to get y-intercept values for lat/lon values */
            InterpolatedLatitude = Functions.adt_slopecal(12.0, 3);
            InterpolatedLongitude = Functions.adt_slopecal(12.0, 4);
            if ((abs(InterpolatedLatitude) > 90.0) || (abs(InterpolatedLongitude) > 180.0)) {
                /* invalid interp and extrap... negative error code returns */
                PositioningMethodID = 0;
                RetID = -1 * RetID;
            } else {
                /* invalid interp but good extrap... positive error code returns */
                PositioningMethodID = 6;
                RetID = 46;
            }
        }

        return new double[] { (double) RetID, InterpolatedLatitude, InterpolatedLongitude,
                InterpolatedIntensity, (double) PositioningMethodID };
    }

}