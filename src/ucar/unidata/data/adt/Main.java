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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger =
            LoggerFactory.getLogger(Main.class);

    public static String HistoryFileName;
    public static String ReturnOutputString;

    public Main() {
        HistoryFileName = null;
        ReturnOutputString = null;
    }

    public int GetInitialPosition() {

        Auto AutoMode = new Auto();

        String ForecastFileName = null;
        double[] AutoMode1Return = null;

        boolean RunAuto = Env.AutoTF;
        ForecastFileName = Env.ForecastFileName;
        int ForecastFileType = Env.ForecastFileType;

        System.out.printf("Run AUTO=%b\n", RunAuto);
        if (RunAuto) {
            try {
                AutoMode1Return = AutoMode.AutoMode1(ForecastFileName, ForecastFileType);

                if (AutoMode1Return == null)
                    return -1;

                if (((int) AutoMode1Return[0]) < 0) {
                    System.out.printf("ERROR with interpolation/extrapolation : return code %d\n",
                            (int) AutoMode1Return[0]);
                    return -1;
                }
            } catch (IOException exception) {
                System.out.printf("ERROR with reading forecast file\n");
                return -2;
            }
            int ForecastReturnFlag = (int) AutoMode1Return[0];
            double ForecastLatitude = AutoMode1Return[1];
            double ForecastLongitude = AutoMode1Return[2];
            double ForecastIntensity = AutoMode1Return[3];
            double ForecastMethodID = AutoMode1Return[4];

            Env.SelectedLatitude = ForecastLatitude;
            Env.SelectedLongitude = ForecastLongitude;

            History.IRCurrentRecord.latitude = ForecastLatitude;
            History.IRCurrentRecord.longitude = ForecastLongitude;
            History.IRCurrentRecord.autopos = 1; // forecast interpolation
            System.out
                    .printf("AutoMode1 output position info : Latitude=%f Longitude=%f Intensity=%f MethodID=%f Flag=%d\n",
                            ForecastLatitude, ForecastLongitude, ForecastIntensity,
                            ForecastMethodID, ForecastReturnFlag);

        } else {
            System.out.printf("Manual Mode : latitude=%f longitude=%f", Env.SelectedLatitude,
                    Env.SelectedLongitude);
            History.IRCurrentRecord.latitude = Env.SelectedLatitude;
            History.IRCurrentRecord.longitude = Env.SelectedLongitude;
            History.IRCurrentRecord.autopos = 0; // Manual
        }

        return 1;

    }

    public void GetARCHERPosition() {

        double[] AutoMode2Return = null;
        double InputLatitude = Env.SelectedLatitude;
        double InputLongitude = Env.SelectedLongitude;
        try {
            AutoMode2Return = Auto.AutoMode2(InputLatitude, InputLongitude);
        } catch (IOException exception) {
            System.out.printf("ERROR with Automode2 routine\n");
            return;
        }

        double FinalLatitude = AutoMode2Return[0];
        double FinalLongitude = AutoMode2Return[1];
        int FinalPositioningMethod = (int) AutoMode2Return[2];
        History.IRCurrentRecord.latitude = FinalLatitude;
        History.IRCurrentRecord.longitude = FinalLongitude;
        History.IRCurrentRecord.autopos = FinalPositioningMethod;

        Env.SelectedLatitude = History.IRCurrentRecord.latitude;
        Env.SelectedLongitude = History.IRCurrentRecord.longitude;

        return;

    }

    public String RunADTAnalysis(boolean RunFullAnalysis, String InputHistoryFile)
            throws IOException {

        String BulletinOutput = null;
        int HistoryFileRecords;

        History CurrentHistory = new History();

        HistoryFileName = InputHistoryFile;

        boolean OverrideScene = Env.OverSceneTF;
        int OverrideSceneTypeValue = Env.OverrideSceneTypeIndex;

        /*
         * System.out.printf("MW Info : Date=%s JulianDate=%d Time=%d Score=%f\n"
         * ,MWDate,Env.MWJulianDate,MWTime,MWScore);
         */

        /* READ HISTORY FILE INFORMATION */
        if (RunFullAnalysis && HistoryFileName != null) {
            try {
                CurrentHistory.ReadHistoryFile(HistoryFileName);
            } catch (IOException exception) {
                System.out.printf("History file %s not found\n", HistoryFileName);
            }
        } else {
            System.out.printf("Not utilizing a history file\n");
        }
        HistoryFileRecords = History.HistoryNumberOfRecords();
        System.out.printf("Number of records in history file %s is %d\n", HistoryFileName,
                HistoryFileRecords);

        /* read topography file at center position */
        double PositionLatitude = History.IRCurrentRecord.latitude;
        double PositionLongitude = History.IRCurrentRecord.longitude;
        String topoPath = new File(".").getCanonicalPath();
        System.err.println("topoPath: " + topoPath);
        String TopoFileName = "/edu/wisc/ssec/mcidasv/resources/digelev_hires_le.map";

        int TopographyFlag = 0;
        System.out.printf("TOPO Info : File=%s Lat=%f Lon=%f\n", TopoFileName, PositionLatitude,
                PositionLongitude);
        try {
            TopographyFlag = Topo.ReadTopoFile(TopoFileName, PositionLatitude, PositionLongitude);
        } catch (IOException e) {
            logger.error("ERROR reading topography file", e);
            return null;
        }
        /* System.out.printf("after topo read flag=%d\n",TopographyFlag); */
        History.IRCurrentRecord.land = TopographyFlag;

        /* Calculate Eye and Cloud region temperatures */
        Data.CalcEyeCloudTemps();
        /* System.out.printf("after calceyecloudtemps\n"); */
        /*
         * double Eye_Temperature = History.IRCurrentRecord.eyet; double
         * CWCloud_Temperature = History.IRCurrentRecord.cwcloudt; double
         * Cloud_Temperature = History.IRCurrentRecord.cloudt; double
         * Cloud2_Temperature = History.IRCurrentRecord.cloudt2; double
         * Cloud_Symmetry = History.IRCurrentRecord.cloudsymave; double Eye_STDV
         * = History.IRCurrentRecord.eyestdv; int CWRing_Distance =
         * History.IRCurrentRecord.cwring;
         * System.out.printf("Eye Temperature=%f\n",Eye_Temperature);
         * System.out.printf("CWCloud Temperature=%f\n",CWCloud_Temperature);
         * System.out.printf("CWRing Distance=%d\n",CWRing_Distance);
         * System.out.printf("Cloud Temperature=%f\n",Cloud_Temperature);
         * System.out.printf("Cloud2 Temperature=%f\n",Cloud2_Temperature);
         * System.out.printf("Cloud Symmetry=%f\n",Cloud_Symmetry);
         * System.out.printf("Eye STDV=%f\n",Eye_STDV);
         */

        /* Calculate Eye and Cloud region Scene Type */

        /*
         * System.out.printf("overridescenetypevalue=%d\n",
         * OverrideSceneTypeValue);
         */
        if (OverrideSceneTypeValue >= 0) {
            /* System.out.printf("setting old scene types\n"); */
            History.IRCurrentRecord.cloudsceneold = History.IRCurrentRecord.cloudscene;
            History.IRCurrentRecord.eyesceneold = History.IRCurrentRecord.eyescene;
            History.IRCurrentRecord.cloudscene = Math.max(0, (OverrideSceneTypeValue - 3));
            History.IRCurrentRecord.eyescene = Math.min(3, OverrideSceneTypeValue);

        } else {
            Scene.DetermineSceneType(RunFullAnalysis);
            /* System.out.printf("after scene type determination\n"); */
            /* System.out.printf("OverrideScene=%b\n",OverrideScene); */
            if (OverrideScene) {
                /*
                 * System.out.printf("overriding scene type : eye=%d cloud=%d\n",
                 * History
                 * .IRCurrentRecord.eyescene,History.IRCurrentRecord.cloudscene
                 * );
                 */
                if (History.IRCurrentRecord.eyescene < 3) {
                    Env.OverrideSceneTypeIndex = History.IRCurrentRecord.eyescene;
                } else {
                    Env.OverrideSceneTypeIndex = 3 + History.IRCurrentRecord.cloudscene;
                }
                /*
                 * System.out.printf("ADTEnv.overridescenetype=%d\n",
                 * Env.OverrideSceneType);
                 */
                return "override";
            }
        }

        /* Calculate Intensity Estimate Values */

        int RedoIntensityFlag = 0;
        Intensity.CalculateIntensity(RedoIntensityFlag, RunFullAnalysis, HistoryFileName);
        /* System.out.printf("after calcintensity\n"); */
        /* Write Bulletin Output */

        BulletinOutput = Output.TextScreenOutput(HistoryFileName);
        /* System.out.printf("\n *** Bulletin Output ***\n%s\n",BulletinOutput); */
        /* System.out.printf("after textscreenoutput\n"); */
        ReturnOutputString = BulletinOutput;

        return ReturnOutputString;

    }
}