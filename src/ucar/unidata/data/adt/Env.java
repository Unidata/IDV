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

import java.lang.String;

import ucar.unidata.idv.control.ADTControl;

public class Env {
    public static final String ADTVersion = "ADT Version 8.2.1";
    public static boolean RunADTTF;
    public static boolean PlotTF;
    public static boolean ListTF;
    public static boolean DeleteTF;
    public static boolean AutoTF;
    public static boolean OverTF;
    public static boolean OverSceneTF;
    public static boolean OverCenterTF;
    public static boolean ATCFOutputTF;
    public static boolean ATCFRecordOutputTF;
    public static boolean CommentAddTF;
    public static boolean RunFullAnalysis;
    public static boolean InitStrengthTF;
    public static boolean LandFlagTF;
    public static boolean CBSearchTF;
    // Default Java boolean value is false - need to initialize if we want true
    public static boolean UseCKZTF = false;
    public static boolean Vmax1or10TF = true;
    public static boolean UsePMWTF;
    public static int UserDefineDomain;        /* 0=automated; 1=manual */
    public static int DomainID;                /* Domain ID value : 0-Auto/Atl;1-Auto/Pac;2-Man/Atl;3-Man/Pac */
    public static int ForecastFileType;
    public static int MWJulianDate;
    public static int MWHHMMSSTime;
    public static int StartJulianDate;
    public static int EndJulianDate;
    public static int StartHHMMSSTime;
    public static int EndHHMMSSTime;
    public static int HistoryListFormat;
    public static int MaxWindSpeedInputID;
    public static int OverrideSceneTypeIndex;
    public static double InitRawTValue;
    public static double RMWSize;
    public static double SubSatLatitude;
    public static double SubSatLongitude;
    public static double MWScore;
    public static double CKZGaleRadius;
    public static double CKZPenv;
    public static double SelectedLatitude;
    public static double SelectedLongitude;
    public static String ASCIIOutputFileName;
    public static String ForecastFileName;
    public static String StormIDString;
    public static String ATCFSourceAgcyIDString;
    public static String CommentString;
    public static int DEBUG;

    public Env() {
        RunADTTF = true;
        PlotTF = false;
        ListTF = false;
        DeleteTF = false;
        AutoTF = false;
        OverTF = false;
        OverSceneTF = false;
        OverCenterTF = false;
        ATCFOutputTF = false;
        ATCFRecordOutputTF = false;
        CommentAddTF = false;
        InitStrengthTF = true;
        LandFlagTF = true;
        CBSearchTF = true;
        UseCKZTF = false;
        Vmax1or10TF = true;
        UsePMWTF = false;

        DomainID = -1;
        ForecastFileType = 0;
        MWJulianDate = 1900001;
        MWHHMMSSTime = 000000;
        StartJulianDate = 1900001;
        EndJulianDate = 000000;
        StartHHMMSSTime = 1900001;
        EndHHMMSSTime = 000000;
        HistoryListFormat = 0;
        MaxWindSpeedInputID = 0;
        // TJJ Mar 2017 - initialize scene type for dropdown to last item in list
        OverrideSceneTypeIndex = ADTControl.SCENE_TYPES.length - 1;

        InitRawTValue = 1.0;
        MWScore = -100000.0;
        CKZGaleRadius = 300.0;
        CKZPenv = 1012.0;
        RMWSize = -99.0;
        SelectedLatitude = -99.5;
        SelectedLongitude = -999.5;

        ASCIIOutputFileName = null;
        ForecastFileName = null;
        StormIDString = null;
        ATCFSourceAgcyIDString = null;
        CommentString = null;

        DEBUG = 1;
    }

}
