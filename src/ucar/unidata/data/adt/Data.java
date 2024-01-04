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

import java.lang.Math;

class IRRingData {
    double distance;
    double angle;
    double temperature;
}

public class Data {

    public static int IRData_NumberRows;
    public static int IRData_NumberColumns;
    public static int IRData_JulianDate;
    public static int IRData_HHMMSSTime;
    public static double IRData_CenterLatitude;
    public static double IRData_CenterLongitude;
    public static double IRData_ImageResolution;
    public static float[][] IRData_Latitude = new float[200][200];
    public static float[][] IRData_Longitude = new float[200][200];
    public static float[][] IRData_Temperature = new float[200][200];
    private static double KtoC_Value = 273.16;
    private static double OUTER_RADIUS = 136.0;
    private static double INNER_RADIUS = 24.0;
    private static double RING_WIDTH = 4.0;
    private static double EYE_SEARCH_RADIUS = 24.0;
    private static int MAXSECTOR = 24;
    private static int MAXSECTORA = 10000;
    private static int TEMPBINS = 64;
    private static int RINGSLICESIZE = 15;
    private static int RingDataArrayNumber;
    private static int CWRing_Distance;
    private static double Eye_Temperature;
    private static double CWCloud_Temperature;
    private static double Cloud_Temperature;
    private static double Cloud2_Temperature;
    private static double Cloud_Symmetry;
    private static double Eye_STDV;
    private static int Eye_FFTValue;
    private static int Cloud_FFTValue;
    private static IRRingData RingArray[] = new IRRingData[40000];
    private static double MaxTempRingArray[] = new double[50];

    public Data() {
        Eye_Temperature = -999.9;
        CWCloud_Temperature = -999.9;
        Cloud_Temperature = -999.9;
        Cloud2_Temperature = -999.9;
        Cloud_Symmetry = -999.9;
        Eye_STDV = -999.9;
        CWRing_Distance = 999;
    }

    private static void LoadRingData(double CenterLatitude, double CenterLongitude) {
        double LatVal, LonVal, TempVal;

        RingDataArrayNumber = 0;
        /*
         * System.out.printf("CenterLat=%f CenterLon=%f\n",CenterLatitude,
         * CenterLongitude);
         */
        /*
         * System.out.printf("numberRows=%d numberColumns=%d\n",IRData_NumberRows
         * ,IRData_NumberColumns);
         */
        for (int j = 0; j < IRData_NumberRows; j++) {
            for (int i = 0; i < IRData_NumberColumns; i++) {
                LatVal = IRData_Latitude[j][i];
                LonVal = IRData_Longitude[j][i];
                TempVal = IRData_Temperature[j][i];
                double LocalValue[] = Functions.distance_angle(LatVal, LonVal, CenterLatitude,
                        CenterLongitude, 1);

                if (LocalValue[0] <= (OUTER_RADIUS + 80.0)) {
                    /*
                     * System.out.printf("j=%d i=%d  lat=%f lon=%f temp=%f ",j,i,
                     * LatVal,LonVal,TempVal);
                     */
                    /*
                     * System.out.printf(
                     * "  Distance=%f Angle=%f  ringarraynumber=%d \n"
                     * ,LocalValue[0],LocalValue[1],RingDataArrayNumber);
                     */
                    RingArray[RingDataArrayNumber] = new IRRingData();

                    RingArray[RingDataArrayNumber].distance = LocalValue[0];
                    RingArray[RingDataArrayNumber].angle = LocalValue[1];
                    RingArray[RingDataArrayNumber].temperature = TempVal;
                    RingDataArrayNumber++;
                }
            }
        }

    }

    private static int RingDataNumberOfPoints() {
        return RingDataArrayNumber;
    }

    private static double CalcEyeTemperature() {
        double EyeMaxTemp = -99.0;

        int RingDataCount = Data.RingDataNumberOfPoints();
        /* System.out.printf("number of points in RingData=%d\n",RingDataCount); */

        for (int i = 0; i < RingDataCount; i++) {
            if (RingArray[i].distance <= EYE_SEARCH_RADIUS) {
                /*
                 * System.out.printf("i=%d distance=%f temp=%f MAX=%f\n",i,RingArray
                 * [i].distance,RingArray[i].temperature,EyeMaxTemp);
                 */
                if (RingArray[i].temperature > EyeMaxTemp) {
                    EyeMaxTemp = RingArray[i].temperature;
                }
            }
        }

        return EyeMaxTemp;
    }

    private static double[] CalcCWCloudInfo() {
        double CWCloudTemp = 10000.0;
        double CWRingDist = 0;
        double DistVal;
        double TempVal;
        int CurrentRing;
        int IntVal;
        int i, j;

        int RingDataCount = Data.RingDataNumberOfPoints();

        int MaxNumberRings = (int) ((OUTER_RADIUS - INNER_RADIUS) / RING_WIDTH);
        /* System.out.printf("maxNumberRings=%d\n",MaxNumberRings); */
        for (j = 0; j < MaxNumberRings; j++) {
            MaxTempRingArray[j] = -999.0;
        }

        for (i = 0; i < RingDataCount; i++) {
            DistVal = RingArray[i].distance;
            TempVal = RingArray[i].temperature;
            if ((DistVal >= INNER_RADIUS) && (DistVal < OUTER_RADIUS)) {
                IntVal = (int) (DistVal - INNER_RADIUS);
                CurrentRing = IntVal / ((int) RING_WIDTH);
                if (TempVal > MaxTempRingArray[CurrentRing]) {
                    MaxTempRingArray[CurrentRing] = TempVal;
                }
            }
        }

        for (j = 0; j < MaxNumberRings; j++) {
            if ((MaxTempRingArray[j] < CWCloudTemp) && (MaxTempRingArray[j] > 160.0)) {
                CWCloudTemp = MaxTempRingArray[j];
                CWRingDist = (((double) j) * RING_WIDTH) + INNER_RADIUS;
            }
            /*
             * System.out.printf(
             * "ring=%d MaxTempRing=%f CWCloudTemp=%f CWRingDist=%f\n"
             * ,j,MaxTempRingArray[j],CWCloudTemp,CWRingDist);
             */
        }

        return new double[] { CWCloudTemp, CWRingDist };

    }

    private static double[] CalcSkew(double[] InputArray, int Counter) {

        int i;
        double ArraySum = 0.0;
        double DifferenceValue = 0.0;
        double ArrayValuesSumSquared = 0.0;
        double STDVValue;

        for (i = 0; i < Counter; i++) {
            ArraySum = ArraySum + InputArray[i];
        }
        double AverageValue = ArraySum / (double) Counter;

        for (i = 0; i < Counter; i++) {
            DifferenceValue = InputArray[i] - AverageValue;
            ArrayValuesSumSquared = ArrayValuesSumSquared + (DifferenceValue * DifferenceValue);
        }

        if (Counter <= 1) {
            STDVValue = 0.0;
        } else {
            STDVValue = Math.sqrt((1.0 / ((double) Counter - 1.0)) * ArrayValuesSumSquared);
        }

        /*
         * System.out.printf("average value=%f  stdv=%f\n",AverageValue,
         * STDVValue);
         */

        return new double[] { AverageValue, STDVValue };
    }

    private static double[] CalcEyeCloudInfo() {

        int i, j;
        double InnerRadiusDistance;
        double OuterRadiusDistance;
        double DistVal, AngleVal, TempVal;
        int SectorCountArray[] = new int[MAXSECTORA];
        double TemperatureHistArray[] = new double[TEMPBINS];
        double TemperatureHistArrayCounter[] = new double[TEMPBINS];
        double TemperatureArray[] = new double[TEMPBINS];
        double SectorDataArray[][] = new double[MAXSECTOR][MAXSECTORA];
        double EyeDataArray[] = new double[MAXSECTORA];
        double SectorAverageArray[] = new double[MAXSECTOR];
        double SectorStdvArray[] = new double[MAXSECTOR];

        int RingDataCount = Data.RingDataNumberOfPoints();

        for (i = 0; i < TEMPBINS; i++) {
            TemperatureHistArray[i] = KtoC_Value + 26.0 - ((double) i) * 2.0;
        }

        /* determine FFT values for Eye and Cloud regions */
        for (int SceneIDFlag = 0; SceneIDFlag <= 1; SceneIDFlag++) {
            for (i = 0; i < TEMPBINS; i++) {
                TemperatureHistArrayCounter[i] = 0.0;
                TemperatureArray[i] = 0.0;
            }

            if (SceneIDFlag == 0) {
                /* CLOUD TOP REGION */
                InnerRadiusDistance = (double) INNER_RADIUS;
                OuterRadiusDistance = (double) OUTER_RADIUS;
            } else {
                /* EYE REGION */
                InnerRadiusDistance = (double) 0;
                OuterRadiusDistance = (double) INNER_RADIUS;
            }

            for (i = 0; i < RingDataCount; i++) {
                DistVal = RingArray[i].distance;
                if ((DistVal >= InnerRadiusDistance) && (DistVal <= OuterRadiusDistance)) {
                    TempVal = RingArray[i].temperature;
                    for (j = 0; j < (TEMPBINS - 1); j++) {
                        if ((TempVal <= TemperatureHistArray[j])
                                && (TempVal > TemperatureHistArray[j + 1])) {
                            TemperatureHistArrayCounter[j] = TemperatureHistArrayCounter[j] + 1.0;
                            TemperatureArray[j] = TemperatureArray[j] + TempVal;
                        }
                    }
                }
            }

            int FFT_ReturnValue = FFT.calculateFFT(TemperatureHistArrayCounter);

            /*
             * System.out.printf("sceneID=%d  harmonic=%d\n",SceneIDFlag,
             * FFT_ReturnValue);
             */

            if (SceneIDFlag == 0) {
                Cloud_FFTValue = FFT_ReturnValue;
            } else {
                Eye_FFTValue = FFT_ReturnValue;
            }
        }

        History.IRCurrentRecord.eyefft = Eye_FFTValue;
        History.IRCurrentRecord.cloudfft = Cloud_FFTValue;

        /* determine various Eye and Cloud region parameters */
        for (i = 0; i < MAXSECTOR; i++) {
            SectorCountArray[i] = 0;
        }

        int EyeCount = 0;
        InnerRadiusDistance = (double) INNER_RADIUS;
        OuterRadiusDistance = (double) OUTER_RADIUS;

        for (i = 0; i < RingDataCount; i++) {
            DistVal = RingArray[i].distance;
            AngleVal = RingArray[i].angle;
            TempVal = RingArray[i].temperature;
            if (AngleVal == 360.0)
                AngleVal = 0.0;
            int SectorVal = 0;

            /* Check for Cloud region pixel */
            if ((DistVal >= InnerRadiusDistance) && (DistVal <= OuterRadiusDistance)) {
                while (SectorVal < MAXSECTOR) {
                    double SectorStartAngle = Math.max(0.0, (((double) SectorVal) * RINGSLICESIZE));
                    double SectorEndAngle = Math.min(360.0,
                            (((double) SectorVal + 1) * RINGSLICESIZE));
                    if ((AngleVal >= SectorStartAngle) && (AngleVal < SectorEndAngle)) {
                        SectorDataArray[SectorVal][SectorCountArray[SectorVal]] = TempVal;
                        SectorCountArray[SectorVal]++;
                        SectorVal = MAXSECTOR; /* exit while loop */
                    } else {
                        SectorVal++;
                    }
                }
            }

            /* Check for Eye region pixel */
            if ((DistVal >= 0.0) && (DistVal < InnerRadiusDistance)) {
                EyeDataArray[EyeCount] = TempVal;
                EyeCount++;
            }
        }

        /* Calculate Cloud Region Annulus Temperature */
        /*
         * position annulus at CW max temp distance and determine mean temp w/in
         * +/- 40km from this distance. If dist is less than 68km from center,
         * annulus will start at 28km
         */
        int AnnulusTemperatureCount = 0;
        double AnnulusTemperatureSum = 0.0;
        double AnnulusDistance = History.IRCurrentRecord.cwring;
        double AnnulusStartRadius = Math.max(28.0, AnnulusDistance - 40.0);
        double AnnulusEndRadius = Math.max(108.0, AnnulusDistance + 40.0);
        for (i = 0; i < RingDataCount; i++) {
            DistVal = RingArray[i].distance;
            AngleVal = RingArray[i].angle;
            TempVal = RingArray[i].temperature;
            if ((DistVal >= AnnulusStartRadius) && (DistVal <= AnnulusEndRadius)) {
                AnnulusTemperatureSum = AnnulusTemperatureSum + TempVal;
                AnnulusTemperatureCount++;
            }
        }

        double CloudAnnulusAveTemp = AnnulusTemperatureSum / ((double) AnnulusTemperatureCount);

        /* calculate averages, standard deviations and skews for each sector */
        double TempSectorArray[] = new double[MAXSECTORA];
        for (i = 0; i < MAXSECTOR; i++) {
            int SectorCounterValue = SectorCountArray[i];
            for (j = 0; j < SectorCounterValue; j++) {
                TempSectorArray[j] = SectorDataArray[i][j];
            }
            double ReturnValues[] = Data.CalcSkew(TempSectorArray, SectorCounterValue);
            SectorAverageArray[i] = ReturnValues[0];
            SectorStdvArray[i] = ReturnValues[1];
        }
        double ReturnValues2[] = Data.CalcSkew(SectorAverageArray, MAXSECTOR);
        double SectorAverageAverageValue = ReturnValues2[0]; /* cloud2 value */

        int HalfMaxSector = MAXSECTOR / 2;
        double SectorDifferenceArray[] = new double[HalfMaxSector];
        for (i = 0; i < HalfMaxSector; i++) {
            SectorDifferenceArray[i] = Math.abs(SectorAverageArray[i]
                    - SectorAverageArray[i + HalfMaxSector]);
        }
        double ReturnValues3[] = Data.CalcSkew(SectorDifferenceArray, HalfMaxSector);
        double SectorDiffAverageValue = ReturnValues3[0]; /*
         * cloud symmetry
         * value
         */

        double ReturnValues4[] = Data.CalcSkew(EyeDataArray, EyeCount);
        double EyeRegionSTDVValue = ReturnValues4[1]; /* eye stdv value */

        return new double[] { CloudAnnulusAveTemp, SectorAverageAverageValue,
                SectorDiffAverageValue, EyeRegionSTDVValue };

    }

    public static void CalcEyeCloudTemps() {

        int CenterXPos = IRData_NumberColumns / 2;
        int CenterYPos = IRData_NumberRows / 2;

        /*
         * System.out.printf("CenterXPos=%d  CenterYPos=%d\n",CenterXPos,CenterYPos
         * );
         */
        double CenterLatValue = IRData_Latitude[CenterYPos][CenterXPos];
        double CenterLonValue = IRData_Longitude[CenterYPos][CenterXPos];

        /*
         * System.out.printf("CenterLatVal=%f  CenterLonVal=%f\n",CenterLatValue,
         * CenterLonValue);
         */

        LoadRingData(CenterLatValue, CenterLonValue);

        Eye_Temperature = Data.CalcEyeTemperature();
        History.IRCurrentRecord.eyet = Eye_Temperature - KtoC_Value;
        /* System.out.printf("eyeT=%f\n",Eye_Temperature); */

        double LocalValue[] = Data.CalcCWCloudInfo();
        CWCloud_Temperature = LocalValue[0];
        CWRing_Distance = (int) LocalValue[1];
        /* System.out.printf("cw cloudT=%f\n",CWCloud_Temperature); */
        /* System.out.printf("cw Ring distance=%d\n",CWRing_Distance); */
        History.IRCurrentRecord.cwcloudt = CWCloud_Temperature - KtoC_Value;
        History.IRCurrentRecord.cwring = CWRing_Distance;

        double LocalValue2[] = Data.CalcEyeCloudInfo();
        Cloud_Temperature = LocalValue2[0];
        Cloud2_Temperature = LocalValue2[1];
        Cloud_Symmetry = LocalValue2[2];
        Eye_STDV = LocalValue2[3];
        /* System.out.printf("cloudt=%f\n",Cloud_Temperature); */
        /* System.out.printf("cloud2t=%f\n",Cloud2_Temperature); */
        /*
         * System.out.printf("eyestdv=%f\n",Eye_STDV); / double check these
         * values
         */
        /* System.out.printf("cloudsymave=%f\n",Cloud_Symmetry); */
        History.IRCurrentRecord.cloudt = Cloud_Temperature - KtoC_Value;
        History.IRCurrentRecord.cloudt2 = Cloud2_Temperature - KtoC_Value;
        History.IRCurrentRecord.cloudsymave = Cloud_Symmetry;
        History.IRCurrentRecord.eyestdv = Eye_STDV;

    }

    public static double[] CalcRMW() {

        int CenterXPos = IRData_NumberColumns / 2;
        int CenterYPos = IRData_NumberRows / 2;
        double RadiusMaxWind = -99.5;
        double EyeSizeRadius = -99.5;
        double ThresholdWarmCloudTemperatureDegK = 223.0;
        double CriticalTemperatureDegK = 228.0;

        /*
         * System.out.printf("CenterXPos=%d  CenterYPos=%d\n",CenterXPos,CenterYPos
         * );
         */

        double CloudTemperature = History.IRCurrentRecord.cloudt;
        double EyeTemperature = History.IRCurrentRecord.eyet;

        int XDirMaximum = Math.min(IRData_NumberColumns, CenterXPos + 320);
        int XDirMinimum = Math.max(0, CenterXPos - 320);
        int YDirMaximum = Math.min(IRData_NumberRows, CenterYPos + 240);
        int YDirMinimum = Math.max(0, CenterYPos - 240);

        if (CloudTemperature >= (ThresholdWarmCloudTemperatureDegK - KtoC_Value)) {
            CriticalTemperatureDegK = KtoC_Value
                    + ((EyeTemperature + (2.0 * CloudTemperature)) / 3.0);
        }
        /* System.out.printf("thresholdT=%f\n",CriticalTemperatureDegK); */

        /* Iterate five times */
        int XInc, YInc;
        int XDirIterationStoredMaximum = 0;
        int XDirIterationStoredMinimum = 0;
        int YDirIterationStoredMaximum = 0;
        int YDirIterationStoredMinimum = 0;
        for (int Iterations = 0; Iterations < 5; Iterations++) {
            /* System.out.printf("Iteration=%d\n",Iterations); */
            XInc = CenterXPos;
            while (IRData_Temperature[CenterYPos][XInc] > CriticalTemperatureDegK) {
                XInc = XInc - 1;
                if (XInc == XDirMinimum) {
                    /* Eyewall not found */
                    return new double[] { RadiusMaxWind, EyeSizeRadius };
                }
            }
            XDirIterationStoredMinimum = XInc;
            XInc = CenterXPos;
            while (IRData_Temperature[CenterYPos][XInc] > CriticalTemperatureDegK) {
                XInc = XInc + 1;
                if (XInc == XDirMaximum) {
                    /* Eyewall not found */
                    return new double[] { RadiusMaxWind, EyeSizeRadius };
                }
            }
            XDirIterationStoredMaximum = XInc;
            YInc = CenterYPos;
            while (IRData_Temperature[YInc][CenterXPos] > CriticalTemperatureDegK) {
                YInc = YInc - 1;
                if (YInc == YDirMinimum) {
                    /* Eyewall not found */
                    return new double[] { RadiusMaxWind, EyeSizeRadius };
                }
            }
            YDirIterationStoredMinimum = YInc;
            YInc = CenterYPos;
            while (IRData_Temperature[YInc][CenterXPos] > CriticalTemperatureDegK) {
                YInc = YInc + 1;
                if (YInc == YDirMaximum) {
                    /* Eyewall not found */
                    return new double[] { RadiusMaxWind, EyeSizeRadius };
                }
            }
            YDirIterationStoredMaximum = YInc;
            CenterXPos = (int) ((((double) (XDirIterationStoredMinimum + XDirIterationStoredMaximum)) / 2.0));
            CenterYPos = (int) ((((double) (YDirIterationStoredMinimum + YDirIterationStoredMaximum)) / 2.0));
        }

        /* System.out.printf("x=%d  y=%d\n",CenterXPos,CenterYPos); */

        double CenterPointLatitude, CenterPointLongitude;
        double LatitudeValue, LongitudeValue;

        CenterPointLatitude = IRData_Latitude[CenterYPos][CenterXPos];
        CenterPointLongitude = IRData_Longitude[CenterYPos][CenterXPos];

        LatitudeValue = IRData_Latitude[CenterYPos][XDirIterationStoredMinimum];
        LongitudeValue = IRData_Longitude[CenterYPos][XDirIterationStoredMinimum];
        double LocalValue1[] = Functions.distance_angle(LatitudeValue, LongitudeValue,
                CenterPointLatitude, CenterPointLongitude, 1);
        double DistanceValueX1 = LocalValue1[0];

        LatitudeValue = IRData_Latitude[CenterYPos][XDirIterationStoredMaximum];
        LongitudeValue = IRData_Longitude[CenterYPos][XDirIterationStoredMaximum];
        double LocalValue2[] = Functions.distance_angle(LatitudeValue, LongitudeValue,
                CenterPointLatitude, CenterPointLongitude, 1);
        double DistanceValueX2 = LocalValue2[0];

        LatitudeValue = IRData_Latitude[YDirIterationStoredMinimum][CenterXPos];
        LongitudeValue = IRData_Longitude[YDirIterationStoredMinimum][CenterXPos];
        double LocalValue3[] = Functions.distance_angle(LatitudeValue, LongitudeValue,
                CenterPointLatitude, CenterPointLongitude, 1);
        double DistanceValueY1 = LocalValue3[0];

        LatitudeValue = IRData_Latitude[YDirIterationStoredMaximum][CenterXPos];
        LongitudeValue = IRData_Longitude[YDirIterationStoredMaximum][CenterXPos];
        double LocalValue4[] = Functions.distance_angle(LatitudeValue, LongitudeValue,
                CenterPointLatitude, CenterPointLongitude, 1);
        double DistanceValueY2 = LocalValue4[0];

        double AveragedDistance = (DistanceValueX1 + DistanceValueX2 + DistanceValueY1 + DistanceValueY2) / 4.0;

        if (AveragedDistance > 0.0) {
            RadiusMaxWind = 2.8068 + (0.8361 * AveragedDistance);
            EyeSizeRadius = AveragedDistance;
        }

        return new double[] { RadiusMaxWind, EyeSizeRadius };

    }

    public static float[][] GetCurrentImageLatitudeArray() {
        return IRData_Latitude;
    }

    public static float[][] GetCurrentImageLongitudeArray() {
        return IRData_Longitude;
    }

    public static float[][] GetCurrentImageTemperatureArray() {
        return IRData_Temperature;
    }

    public static int GetCurrentImageJulianDate() {
        return IRData_JulianDate;
    }

    public static int GetCurrentImageTime() {
        return IRData_HHMMSSTime;
    }

    public static int GetCurrentImageXSize() {
        return IRData_NumberColumns;
    }

    public static int GetCurrentImageYSize() {
        return IRData_NumberRows;
    }

    public static double GetCurrentImageResolution() {
        return IRData_ImageResolution;
    }

    public static double GetStormCenterLatitude() {
        return IRData_CenterLatitude;
    }

    public static double GetStormCenterLongitude() {
        return IRData_CenterLongitude;
    }

}