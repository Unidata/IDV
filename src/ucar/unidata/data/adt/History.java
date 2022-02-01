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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IRHistoryRecord {
    int date;
    int time;
    double TrawO;
    double Traw;
    double Tfinal;
    double CI;
    double eyet;
    double cloudt;
    double cloudt2;
    double cwcloudt;
    double latitude;
    double longitude;
    double eyecdosize;
    double eyestdv;
    double cloudsymave;
    int sattype;
    int eyescene;
    int cloudscene;
    int eyesceneold;
    int cloudsceneold;
    int rule9;
    int rule8;
    int land;
    int eyefft;
    int cloudfft;
    int ringcb;
    int ringcbval;
    int cwring;
    int ringcbvalmax;
    double ringcbvalmaxlat;
    double ringcbvalmaxlon;
    double CIadjp;
    int autopos;
    int LBflag;
    int rapiddiss;
    double rmw;
    double mwscore;
    int mwdate;
    int mwtime;
    int r34;
    int MSLPenv;
    int vza;
    String comment;
}

public class History {

    private static final Logger logger = LoggerFactory.getLogger(History.class);
    public static int HistoryFileRecords;
    public static IRHistoryRecord HistoryFile[] = new IRHistoryRecord[1000];
    public static IRHistoryRecord IRCurrentRecord = new IRHistoryRecord();
    /** ATCF Rule 8/9 array */
    static String[] Rule89_ATCF = { "  ", "R8", "R9", "89" };
    /** ATCF Rule 9 array */
    static String[] Rule9String = { "OFF", " ON", "WKN", "N/A" };
    /** rapid dissipation array */
    static String[] RapidDissString = { "OFF", "FLG", "ON ", "ON ", "N/A" };
    /** eye scenes */
    static String[] EyeSceneString = { "EYE   ", "EYE/P ", "EYE/L ", "EYE/LR", "EYE/R ", "EYE/OB" };
    /** cloud scenes */
    static String[] CloudSceneString = { "UNIFRM", "EMBC  ", "IRRCDO", "CRVBND", "SHEAR ", "EYE MW" };
    /** ATCF cloud */
    static String[] CloudSceneString_ATCF = { " CDO", "EMBC", "ICDO", "CBND", "SHER", "MEYE" };
    static String[] AutoPosStringAbbr = { " MAN ", "FCST ", "LAPL ", "WARM ", "SPRL ", "COMBO",
            "EXTRP", "NETCDF", " N/A " };
    static String[] Rule8String = { "NO LIMIT ", "0.5T/6hr ", "1.0T/6hr ", "1.7T/12hr",
            "2.2T/18hr", "2.7T/24hr", "         ", "         ", "0.2T/hour", "0.5T/hour",
            "NO LIMIT ", "0.5T/6hr ", "1.0T/6hr ", "2.7T/12hr", "3.2T/18hr", "3.7T/24hr",
            "         ", "         ", "0.2T/hour", "0.5T/hour", "NO LIMIT ", "0.5T/6hr ",
            "0.7T/6hr ", "1.2T/12hr", "1.7T/18hr", "2.2T/24hr", "         ", "         ",
            "0.2T/hour", "0.5T/hour", "MW Adjst ", " MW ON    ", "MW ON    ", "MW HOLD  ",
            "MW AdjEnd" };
    static int HISTLEN = 194;

    /*
     * public static IRHistoryRecord IRCurrentRecord = new IRHistoryRecord(); /
     * this could be public if I redo everything
     */

    public History() {
        HistoryFileRecords = 0;
    }

    public static int HistoryNumberOfRecords() {
        return HistoryFileRecords;
    }

    /**
     * List the ASCII history file between given date/times.
     *
     * @param OutputStyle
     *            Output one record (-1) or entire file (0).
     * @param OutputFormatTypeID
     *            0=ATCF, -1=History List.
     * @param ATCFFileSourceIDString
     *            ATCF file source identifier.
     * @param ATCFStormIDString
     *            ATCF storm identifier.
     *
     * @return Matching part of the ASCII history file.
     */

    public static String ListHistory(int OutputStyle, int OutputFormatTypeID,
                                     String ATCFFileSourceIDString, String ATCFStormIDString) {
        String HistoryFileListing = "";

        boolean UseCKZTF = Env.UseCKZTF;
        boolean Vmax1or10TF = Env.Vmax1or10TF;
        int NumRecsHistory;
        int DateValue;
        int TimeValue;
        double RawTAdj;
        double RawTOrig;
        double FinalT;
        double CI;
        double EyeTemp;
        double CloudTemp;
        double Latitude;
        double Longitude;
        int SatelliteIDValue;
        int CloudScene;
        int EyeScene;
        int LandFlag;
        int Rule8Flag;
        int Rule9Flag;
        int RapidDissFlag;
        double CIPresAdj;
        double RadiusMaxWind;
        double MWEyeScore;
        int AutoCenteringValue;
        int R34Distance;
        int EnvironMSLP;
        double SatVZA;
        String CommentString = null;

        logger.debug("outputstyle={} outputformattype={}", OutputStyle, OutputFormatTypeID);

        if (OutputStyle == 0) {
            NumRecsHistory = HistoryNumberOfRecords(); // loop through history
            // file
        } else {
            NumRecsHistory = 1; // output current record
        }

        int XInc = 0;
        while (XInc < NumRecsHistory) {
            if (XInc == 0) {
                if (OutputFormatTypeID == -1) {
                    /* original format history file listing */
                    if (UseCKZTF) {
                        HistoryFileListing += String.format("                  ----Intensity--- "
                                + "-Tno Values-- ---Tno/CI Rules---  -Temperature-"
                                + "                    \n");
                    } else {
                        HistoryFileListing += String
                                .format("                  --------Intensity------- "
                                        + "-Tno Values-- ---Tno/CI Rules---  -Temperature-"
                                        + "                    \n");
                    }
                    String LabelA = "";
                    String LabelB = "";
                    if (UseCKZTF) {
                        if (Vmax1or10TF) {
                            LabelA = String.format("      MSLP/Vmax   ");
                        } else {
                            LabelA = String.format("%18s", "      MSLP/Vmax10 ");
                        }
                        HistoryFileListing += String.format("           Time  %18s "
                                + "Fnl Adj Ini   Cnstrnt Wkng Rpd    Cntr   Mean   "
                                + "Scene  EstRMW   MW   Storm Location  Fix\n", LabelA);
                    } else {
                        if (Vmax1or10TF) {
                            LabelB = String.format("%24s", "     MSLP/MSLPLat/Vmax  ");
                        } else {
                            LabelB = String.format("%24s", "     MSLP/MSLPLat/Vmax10");
                        }
                        HistoryFileListing += String.format("           Time    %24s "
                                + "Fnl Adj Ini   Cnstrnt Wkng Rpd    Cntr   Mean   "
                                + "Scene  EstRMW   MW   Storm Location  Fix\n", LabelB);
                    }
                    if (UseCKZTF) {
                        HistoryFileListing += String.format("   Date    (UTC)   CI  (CKZ)/(kts)  "
                                + "Tno Raw Raw    Limit  Flag Wkng  Region  Cloud  "
                                + "Type    (km)  Score   Lat     Lon    Mthd    Sat   "
                                + "VZA  Comments\n");
                    } else {
                        HistoryFileListing += String
                                .format("   Date    (UTC)   CI  (DvT)/BiasAdj/(kts)  "
                                        + "Tno Raw Raw    Limit  Flag Wkng  Region  Cloud  "
                                        + "Type    (km)  Score   Lat     Lon    Mthd     Sat   "
                                        + "VZA  Comments\n");
                    }
                } else {
                    /* ATCF format listing */
                    /* HistoryFileListing += String.format("\n"); */
                }
            }
            if (OutputStyle == 0) {
                DateValue = HistoryFile[XInc].date;
                TimeValue = HistoryFile[XInc].time;
                RawTAdj = HistoryFile[XInc].Traw;
                RawTOrig = HistoryFile[XInc].TrawO;
                FinalT = HistoryFile[XInc].Tfinal;
                CI = HistoryFile[XInc].CI;
                EyeTemp = HistoryFile[XInc].eyet;
                CloudTemp = HistoryFile[XInc].cloudt;
                Latitude = HistoryFile[XInc].latitude;
                Longitude = HistoryFile[XInc].longitude;
                SatelliteIDValue = HistoryFile[XInc].sattype;
                CloudScene = HistoryFile[XInc].cloudscene;
                EyeScene = HistoryFile[XInc].eyescene;
                LandFlag = HistoryFile[XInc].land;
                Rule8Flag = HistoryFile[XInc].rule8;
                Rule9Flag = HistoryFile[XInc].rule9;
                RapidDissFlag = HistoryFile[XInc].rapiddiss;
                CIPresAdj = HistoryFile[XInc].CIadjp;
                RadiusMaxWind = HistoryFile[XInc].rmw;
                MWEyeScore = HistoryFile[XInc].mwscore;
                AutoCenteringValue = HistoryFile[XInc].autopos;
                R34Distance = HistoryFile[XInc].r34;
                EnvironMSLP = HistoryFile[XInc].MSLPenv;
                SatVZA = ((float) HistoryFile[XInc].vza) / 10.0;
                CommentString = String.format("%s", HistoryFile[XInc].comment);
            } else {
                DateValue = IRCurrentRecord.date;
                TimeValue = IRCurrentRecord.time;
                RawTAdj = IRCurrentRecord.Traw;
                RawTOrig = IRCurrentRecord.TrawO;
                FinalT = IRCurrentRecord.Tfinal;
                CI = IRCurrentRecord.CI;
                EyeTemp = IRCurrentRecord.eyet;
                CloudTemp = IRCurrentRecord.cloudt;
                Latitude = IRCurrentRecord.latitude;
                Longitude = IRCurrentRecord.longitude;
                SatelliteIDValue = IRCurrentRecord.sattype;
                CloudScene = IRCurrentRecord.cloudscene;
                EyeScene = IRCurrentRecord.eyescene;
                LandFlag = IRCurrentRecord.land;
                Rule8Flag = IRCurrentRecord.rule8;
                Rule9Flag = IRCurrentRecord.rule9;
                RapidDissFlag = IRCurrentRecord.rapiddiss;
                CIPresAdj = IRCurrentRecord.CIadjp;
                RadiusMaxWind = IRCurrentRecord.rmw;
                MWEyeScore = IRCurrentRecord.mwscore;
                AutoCenteringValue = IRCurrentRecord.autopos;
                R34Distance = IRCurrentRecord.r34;
                EnvironMSLP = IRCurrentRecord.MSLPenv;
                SatVZA = ((float) IRCurrentRecord.vza) / 10.0;
                CommentString = String.format("%s", IRCurrentRecord.comment);
            }

            boolean ListLandRecordTF = true;
            if ((LandFlag == 1) && (CI < 1.0)) {
                ListLandRecordTF = false;
            }

            double CIPressureValue;
            double CIWindValue;
            String RadiusMaxWindString = "";
            String MWScoreString = "";
            String SceneTypeString = "";
            if (!ListLandRecordTF) {
                CIPresAdj = 0.0;
                CIPressureValue = 0.0;
                CIWindValue = 0.0;
                SceneTypeString = String.format("LAND  ");
                Rule8Flag = 6;
                Rule9Flag = 3;
                RapidDissFlag = 4;
                /* AutoCenteringValue=7; */
                RadiusMaxWindString = String.format("  N/A ");
                MWScoreString = String.format("  N/A");
            } else {
                Env.CKZGaleRadius = R34Distance;
                Env.CKZPenv = EnvironMSLP;
                CIPressureValue = Functions.adt_getpwval(0, CI, Latitude, Longitude);
                CIWindValue = Functions.adt_getpwval(1, CI, Latitude, Longitude);
                if (!Vmax1or10TF) {
                    /* convert 1-minute to 10-minute average Vmax for output */
                    CIWindValue = 0.88 * CIWindValue;
                }
                if ((CloudScene == 3) || (CloudScene == 4)) {
                    SceneTypeString = String.format("%s", CloudSceneString[CloudScene]);
                } else if (EyeScene < 3) {
                    SceneTypeString = String.format("%s", EyeSceneString[EyeScene]);
                } else {
                    SceneTypeString = String.format("%s", CloudSceneString[CloudScene]);
                }
                if ((CloudScene <= 5) && (EyeScene <= 2)) {
                    RadiusMaxWindString = String.format("%3d IR", (int) RadiusMaxWind);
                } else {
                    RadiusMaxWindString = String.format("%s", "  N/A ");
                }
                if (MWEyeScore >= -99.0) {
                    MWScoreString = String.format("%5.1f", MWEyeScore);
                } else {
                    MWScoreString = String.format("%s", "  N/A");
                }
            }

            logger.debug("here AA {} {}", OutputFormatTypeID, NumRecsHistory);
            if (OutputFormatTypeID == -1) {
                /* original format history file listing */
                String DateString = Functions.adt_julian2cmonth(DateValue);
                String LatLonComboString = String.format("%6.2f %7.2f", Latitude, Longitude);
                String SatelliteIDString = Functions.adt_sattypes(SatelliteIDValue);
                if (UseCKZTF) {
                    HistoryFileListing += String.format("%9s %06d  %3.1f %6.1f %5.1f  "
                                    + "%3.1f %3.1f %3.1f  %8s %3s  %3s  "
                                    + "%6.2f %6.2f  %6s %6s %5s %15s  %5s %7s %4.1f %s\n", DateString,
                            TimeValue, CI, CIPressureValue, CIWindValue, FinalT, RawTAdj, RawTOrig,
                            Rule8String[Rule8Flag], Rule9String[Rule9Flag],
                            RapidDissString[RapidDissFlag], EyeTemp, CloudTemp, SceneTypeString,
                            RadiusMaxWindString, MWScoreString, LatLonComboString,
                            AutoPosStringAbbr[AutoCenteringValue], SatelliteIDString, SatVZA,
                            CommentString);
                } else {
                    HistoryFileListing += String.format("%9s %06d  %3.1f %6.1f  %+5.1f  %5.1f  "
                                    + "%3.1f %3.1f %3.1f  %8s %3s  %3s  "
                                    + "%6.2f %6.2f  %6s %6s %5s %15s  %5s %7s %4.1f %s\n", DateString,
                            TimeValue, CI, CIPressureValue + CIPresAdj, CIPresAdj, CIWindValue,
                            FinalT, RawTAdj, RawTOrig, Rule8String[Rule8Flag],
                            Rule9String[Rule9Flag], RapidDissString[RapidDissFlag], EyeTemp,
                            CloudTemp, SceneTypeString, RadiusMaxWindString, MWScoreString,
                            LatLonComboString, AutoPosStringAbbr[AutoCenteringValue],
                            SatelliteIDString, SatVZA, CommentString);
                }
            } else {
                /* ATCF format listing */
                int RawTFlag_ATCF = OutputFormatTypeID / 1000;
                int[] ReturnValues = Functions.adt_yddmy(DateValue);
                int MonthValue = ReturnValues[0];
                int DayValue = ReturnValues[1];
                int YearValue = ReturnValues[2];
                String TimeString_ATCF = String.format("%04d", TimeValue / 100);
                String DateString_ATCF = String.format("%4d%02d%02d%4s", YearValue, MonthValue,
                        DayValue, TimeString_ATCF);
                String NSString_ATCF = (Latitude > 0.0) ? String.format("%s", "N") : String.format(
                        "%s", "S");
                String EWString_ATCF = (Longitude > 0.0) ? String.format("%s", "W") : String
                        .format("%s", "E");
                String LatitudeString = String.format("%4d%1s", (int) (Math.abs(Latitude) * 100),
                        NSString_ATCF);
                String LongitudeString = String.format("%5d%1s", (int) (Math.abs(Longitude) * 100),
                        EWString_ATCF);

                int StormIDNum = Integer.parseInt(ATCFStormIDString.substring(0, 2));
                char aChar = ATCFStormIDString.charAt(2);
                String StormIDNumString = String.format("%02d", StormIDNum);

                String BasinIDString_ATCF = "";
                if (aChar == 'L') {
                    BasinIDString_ATCF = String.format("%s", "AL");
                } else if (aChar == 'E') {
                    BasinIDString_ATCF = String.format("%s", "EP");
                } else if (aChar == 'C') {
                    BasinIDString_ATCF = String.format("%s", "CP");
                } else if (aChar == 'W') {
                    BasinIDString_ATCF = String.format("%s", "WP");
                } else if (aChar == 'S') {
                    BasinIDString_ATCF = String.format("%s", "SH");
                } else if (aChar == 'P') {
                    BasinIDString_ATCF = String.format("%s", "SH");
                } else if (aChar == 'U') {
                    BasinIDString_ATCF = String.format("%s", "SH");
                } else if (aChar == 'R') {
                    BasinIDString_ATCF = String.format("%s", "SH");
                } else if (aChar == 'F') {
                    BasinIDString_ATCF = String.format("%s", "SH");
                } else if (aChar == 'B') {
                    BasinIDString_ATCF = String.format("%s", "IO");
                } else if (aChar == 'A') {
                    BasinIDString_ATCF = String.format("%s", "IO");
                } else {
                    BasinIDString_ATCF = String.format("%s", "XX");
                    StormIDNumString = "XX";
                }

                int MaxWindSpeed_ATCF = (int) (CIWindValue);
                int Pressure_ATCF = (int) (CIPressureValue + CIPresAdj);
                int CI_ATCF = (int) ((CI + 0.01) * 10);
                int RawTnoValue_ATCF = 0;
                if (RawTFlag_ATCF == 0) {
                    RawTnoValue_ATCF = (int) ((RawTAdj + 0.01) * 10); /*
                     * adjusted
                     * Raw T#
                     * -
                     * default
                     */
                } else {
                    RawTnoValue_ATCF = (int) ((RawTOrig + 0.01) * 10); /*
                     * unadjusted
                     * Raw T#
                     */
                }
                int FinalTnoValue_ATCF = (int) ((FinalT + 0.01) * 10);
                String TnoAveTimeFlagString_ATCF = String.format("%s", "L");
                int TnoAveTimeFlag_ATCF = 3; /* Final T# - default */
                int EyeTempValue = (int) (EyeTemp);
                int CloudTempValue = (int) (CloudTemp);
                SceneTypeString = "";
                int CIConfidence = 0;
                int WindSpeedConfidence = 0;
                int PressureConfidence = 0;
                int PositionConfidence = 0;
                int Rule89Value = 0;

                if (ListLandRecordTF) {
                    SceneTypeString = String.format("%s", "LAND");
                    CIConfidence = 3;
                    WindSpeedConfidence = 3;
                    PressureConfidence = 3;
                    PositionConfidence = 2;
                    Rule89Value = 0;
                } else {
                    Rule89Value = 0;
                    if ((Rule8Flag % 10) > 0) {
                        Rule89Value = 1;
                    }
                    if (Rule9Flag == 1) {
                        Rule89Value = 2;
                    }
                    if (((Rule8Flag % 10) > 0) && (Rule9Flag == 1)) {
                        Rule89Value = 3;
                    }
                    if ((CloudScene == 3) || (CloudScene == 4)) {
                        SceneTypeString = String.format("%s", CloudSceneString_ATCF[CloudScene]);
                        CIConfidence = 2;
                        WindSpeedConfidence = 2;
                        PressureConfidence = 2;
                        PositionConfidence = 3;
                    } else if (EyeScene < 3) {
                        SceneTypeString = String.format("%s", " EYE");
                        CIConfidence = 1;
                        WindSpeedConfidence = 1;
                        PressureConfidence = 1;
                        PositionConfidence = 1;
                    } else {
                        SceneTypeString = String.format("%s", CloudSceneString_ATCF[CloudScene]);
                        CIConfidence = 2;
                        WindSpeedConfidence = 2;
                        PressureConfidence = 2;
                        PositionConfidence = 2;
                    }
                }
                String SatelliteIDString = Functions.adt_sattypes(SatelliteIDValue);

                String SiteID_ATCF = String.format("%s", ATCFFileSourceIDString);

                /* determine ATCF center/intensity ID */
                String CenterFixMethodID_ATCF = "";
                if (AutoCenteringValue >= 4) {
                    /* center fix by autofix method */
                    CenterFixMethodID_ATCF = String.format("%s", "CI");
                } else {
                    /* center fix by forecast interpolation */
                    if (ListLandRecordTF) {
                        /* storm is over land */
                        CenterFixMethodID_ATCF = String.format("%s", " N");
                    } else {
                        /* storm is over ocean */
                        CenterFixMethodID_ATCF = String.format("%s", " I");
                    }
                }
                String AutoCenterMethodString_ATCF = "";
                if (AutoCenteringValue == 0) {
                    AutoCenterMethodString_ATCF = String.format("%s", "MAN");
                } else {
                    AutoCenterMethodString_ATCF = String.format("%s", "AUT");
                }
                HistoryFileListing += String.format("%2s, %2s, %12s, %3d, %4s, %10s, %1s, "
                                + "%5s, %6s, %5s, %1d, %3d, %1d, %4d, %1d, %4s, "
                                + "%3s, %4s, %4s, %4s, %4s, %4s, %1s, %1s, %1s, %1s, "
                                + "%1s, %3s, %3s, %1s, " + "%5s, %3s, %4s, %2d, %1d, %2d, "
                                + "%3d, %1s, %2d, %4d, %4d, %4s, %2s, " + "%6s, %1s, %s\n",
                        BasinIDString_ATCF, StormIDNumString, DateString_ATCF, 20, "DVTO",
                        CenterFixMethodID_ATCF, " ", LatitudeString, LongitudeString, " ",
                        PositionConfidence, MaxWindSpeed_ATCF, WindSpeedConfidence, Pressure_ATCF,
                        PressureConfidence, "DVRK", " ", " ", " ", " ", " ", " ", " ", " ", " ",
                        " ", " ", " ", " ", " ", SiteID_ATCF, AutoCenterMethodString_ATCF, "   I",
                        CI_ATCF, CIConfidence, FinalTnoValue_ATCF, TnoAveTimeFlag_ATCF,
                        TnoAveTimeFlagString_ATCF, RawTnoValue_ATCF, EyeTempValue, CloudTempValue,
                        SceneTypeString, Rule89_ATCF[Rule89Value], SatelliteIDString, "T",
                        CommentString);
            }

            XInc++;
        }
        return HistoryFileListing;
    }

    /**
     * Insert or overwrite a record in a history file.
     *
     * Global structure HistoryRecordPtr will be modified and the ASCII history
     * file will be rewritten in another routine.
     *
     * @param RunFullAnalysis
     *            Full analysis toggle.
     * @param HistoryFileName
     *            File to modify.
     *
     * @return Array of two integers. The first value represents the number of
     *         inserted records. The second value is flag that describes the
     *         {@literal "type"} of modification (possible values are 1, 2, 3,
     *         4).
     *         <ol>
     *         <li>Overwritten</li>
     *         <li>Inserted</li>
     *         <li>Record placed at start of new history structure.</li>
     *         <li>Record placed at end of existing history structure.</li>
     *         </ol>
     */
    // TODO(jon): document what the flags mean
    public static int[] InsertHistoryRecord(boolean RunFullAnalysis, String HistoryFileName) {

        int ModifiedCount = 0;
        int InsertOverwriteFlag = 0;
        boolean FoundRecordTF = false;
        IRHistoryRecord TemporaryIRCurrentRecord = IRCurrentRecord;

        boolean LandFlagTF = Env.LandFlagTF;

        int NumRecsHistory = HistoryNumberOfRecords();

        int ImageDate = IRCurrentRecord.date;
        int ImageTime = IRCurrentRecord.time;
        double CurrentTime = Functions.calctime(ImageDate, ImageTime);

        int XInc = 0;
        while (XInc < NumRecsHistory) {
            int RecDate = HistoryFile[XInc].date;
            int RecTime = HistoryFile[XInc].time;
            double HistoryRecTime = Functions.calctime(RecDate, RecTime);
            if ((HistoryRecTime == CurrentTime) && (!FoundRecordTF)) {
                /* OVERWRITE RECORD */
                logger.debug("OVERWRITE RECORD {}", XInc);
                HistoryFile[XInc] = IRCurrentRecord;
                FoundRecordTF = true;
                InsertOverwriteFlag = 1;
            } else if ((HistoryRecTime > CurrentTime) && !FoundRecordTF) {
                /* INSERT RECORD */
                logger.debug("INSERT RECORD");
                /* shift records after HistoryRecTime up one record */
                NumRecsHistory++;
                HistoryFileRecords++;
                int YInc = HistoryFileRecords;
                while (YInc > XInc) {
                    HistoryFile[YInc] = HistoryFile[YInc - 1];
                    YInc--;
                }
                HistoryFile[XInc] = IRCurrentRecord;
                /*
                 * System.out.printf("IRcurrentrecord.land=%d\n",IRCurrentRecord.
                 * land);
                 */
                FoundRecordTF = true;
                InsertOverwriteFlag = 2;
            } else {
                if (FoundRecordTF) {
                    logger.debug("RECOMPUTING RECORD {}", XInc);
                    /*
                     * previously found records to insert, so all records
                     * following the inserted record must be recalculated
                     */
                    /*
                     * assign XInc history file record as "current record" and
                     * recalculate intensity
                     */
                    IRCurrentRecord = HistoryFile[XInc];
                    int RecLand = HistoryFile[XInc].land;
                    double RecTnoRaw = HistoryFile[XInc].Traw;
                    if ((LandFlagTF && (RecLand == 1)) || (RecTnoRaw < 1.0)) {
                        /* assign as "missing record" */
                        InitCurrent(false);
                    } else {
                        /* recompute intensity */
                        Intensity.CalculateIntensity(1, RunFullAnalysis, HistoryFileName);
                    }
                    ModifiedCount++;
                }
                /* nothing yet... keep searching */
            }
            XInc++;
        }
        if (!FoundRecordTF) {
            if (XInc == 0) {
                /* record will be placed at start of new history structure */
                logger.debug("PLACE RECORD AT START OF NEW");
                HistoryFile[0] = IRCurrentRecord;
                InsertOverwriteFlag = 3;
            } else {
                /* record will be placed at end of history structure */
                logger.debug("PLACE RECORD AT END OF EXISTING");
                HistoryFile[NumRecsHistory] = IRCurrentRecord;
                InsertOverwriteFlag = 4;
            }
            HistoryFileRecords++;
        } else {
            IRCurrentRecord = TemporaryIRCurrentRecord;
        }

        return new int[] { ModifiedCount, InsertOverwriteFlag };
    }

    /**
     * Delete record(s) in a history file.
     *
     * <p>
     * Routine will modify structure HistoryRecordPtr, which will then be
     * rewritten to ASCII history file in another subroutine.
     * </p>
     *
     * @param RunFullAnalysis
     *            Full analysis toggle.
     * @param HistoryFileName
     *            File to modify.
     *
     * @return Array containing two integer values. The first value is the
     *         number of modified records, and the seconds value is the number
     *         of deleted records.
     */
    public static int[] DeleteHistoryRecords(boolean RunFullAnalysis, String HistoryFileName) {
        int YInc = 0;
        int ModifiedCount = 0;
        int DeleteRecordCount = 0;
        boolean FoundRecordTF = false;
        IRHistoryRecord TemporaryIRCurrentRecord = IRCurrentRecord;
        boolean LandFlagTF = Env.LandFlagTF;

        int NumRecsHistory = HistoryNumberOfRecords();

        int XInc = 0;
        int DateStart = Env.StartJulianDate;
        int DateEnd = Env.EndJulianDate;
        int DateStartTime = Env.StartHHMMSSTime;
        int DateEndTime = Env.EndHHMMSSTime;
        double AnalysisStartTime = Functions.calctime(DateStart, DateStartTime);
        double AnalysisEndTime = Functions.calctime(DateEnd, DateEndTime);

        while (XInc < NumRecsHistory) {
            int RecDate = HistoryFile[XInc].date;
            int RecTime = HistoryFile[XInc].time;
            double HistoryRecTime = Functions.calctime(RecDate, RecTime);

            if ((HistoryRecTime >= AnalysisStartTime) && (HistoryRecTime <= AnalysisEndTime)) {
                /* record falls within time boundaries to delete */
                HistoryFileRecords--;
                NumRecsHistory--;
                DeleteRecordCount++;
                YInc = XInc;
                while (YInc < NumRecsHistory) {
                    HistoryFile[YInc] = HistoryFile[YInc + 1];
                    YInc++;
                }
                FoundRecordTF = true;
                XInc--;
            } else {
                /* record not within time boundaries */
                if (FoundRecordTF) {
                    /*
                     * previously found records to delete, so all records
                     * following the last deleted record must be recalculated
                     */
                    IRCurrentRecord = HistoryFile[XInc];
                    int RecLand = HistoryFile[XInc].land;
                    double RecTnoRaw = HistoryFile[XInc].Traw;
                    if (((LandFlagTF) && (RecLand == 1)) || (RecTnoRaw < 1.0)) {
                        /* assign as "missing record" */
                        InitCurrent(false);
                    } else {
                        /* recompute intensity */
                        Intensity.CalculateIntensity(1, RunFullAnalysis, HistoryFileName);
                    }
                    ModifiedCount++;
                }
            }
            XInc++;
        }

        if (FoundRecordTF) {
            IRCurrentRecord = TemporaryIRCurrentRecord;
        }

        return new int[] { ModifiedCount, DeleteRecordCount };
    }

    /**
     * Insert comment in history file at specifified date/time.
     *
     * @param CommentString
     *            Character string field to be inserted.
     *
     * @return If {@code -1}, there was an error finding record, if {@code >= 0}
     *         , number of modified records.
     */
    public static int CommentHistoryRecords(String CommentString) {

        int RecordCount = 0; /* record counter */
        boolean FoundRecordTF = false; /* found recurd logical */

        int NumRecsHistory = HistoryNumberOfRecords();

        int XInc = 0;
        int DateStart = Env.StartJulianDate;
        int DateEnd = Env.EndJulianDate;
        int DateStartTime = Env.StartHHMMSSTime;
        int DateEndTime = Env.EndHHMMSSTime;
        double AnalysisStartTime = Functions.calctime(DateStart, DateStartTime);
        double AnalysisEndTime = Functions.calctime(DateEnd, DateEndTime);

        while (XInc < NumRecsHistory) {
            int RecDate = HistoryFile[XInc].date;
            int RecTime = HistoryFile[XInc].time;
            double HistoryRecTime = Functions.calctime(RecDate, RecTime);

            if ((HistoryRecTime >= AnalysisStartTime) && (HistoryRecTime <= AnalysisEndTime)) {
                RecordCount++;
                HistoryFile[XInc].comment = String.format("%s", CommentString);
                FoundRecordTF = true;
            }
            XInc++;
        }
        if (!FoundRecordTF) {
            RecordCount = -1;
        }
        return RecordCount;
    }

    /**
     * Write HistoryRecordPtr structure to ASCII history file.
     *
     * @param HistoryFileName
     *            File to write.
     *
     * @return If {@code >= 0}, the value represents the number of records
     *         written. If -2, there was an error reading
     *         {@code HistoryFileName}. If {@code -3}, there was an error
     *         writing {@code HistoryFileName}. Finally, if {@code -5}, there
     *         was an error closing {@code HistoryFileName}.
     *
     * @throws IOException
     *             if there was a problem writing to {@code HistoryFileName}.
     */
    public static int WriteHistoryFile(String HistoryFileName) throws IOException {
        String OutputString;
        FileWriter FileWriteStream = new FileWriter(HistoryFileName, false);
        BufferedWriter HistoryFilePtr = new BufferedWriter(FileWriteStream);

        int RecordCount = 0; /* record counter */

        int NumRecsHistory = HistoryNumberOfRecords();

        int XInc = 0;
        while (XInc < NumRecsHistory) {
            int RecDate = HistoryFile[XInc].date;
            int RecTime = HistoryFile[XInc].time;
            double HistoryRecTime = Functions.calctime(RecDate, RecTime);
            String DateString = Functions.adt_julian2cmonth(RecDate);

            int MWRecDate = HistoryFile[XInc].mwdate;
            String MWDateString = Functions.adt_julian2cmonth(MWRecDate);

            double JulianDate = HistoryRecTime - (double) ((int) HistoryRecTime / 1000) * 1000.0;

            OutputString = String.format("%9s %06d %8.4f %3.1f %3.1f %3.1f %3.1f %6.2f "
                            + "%6.2f %6.2f %6.2f %6.2f %7.2f %5.1f %4.1f %4.1f "
                            + "%3d %1d %1d %2d %2d %1d %02d %1d %1d %1d %2d "
                            + "%2d %1d %2d %3d %2d %6.2f %5.1f %5.1f " + "%9s %06d %4d %4d %3d %s\n",
                    DateString, HistoryFile[XInc].time, JulianDate, HistoryFile[XInc].TrawO,
                    HistoryFile[XInc].Traw, HistoryFile[XInc].Tfinal, HistoryFile[XInc].CI,
                    HistoryFile[XInc].eyet, HistoryFile[XInc].cloudt, HistoryFile[XInc].cloudt2,
                    HistoryFile[XInc].cwcloudt, HistoryFile[XInc].latitude,
                    HistoryFile[XInc].longitude, HistoryFile[XInc].eyecdosize,
                    HistoryFile[XInc].eyestdv, HistoryFile[XInc].cloudsymave,
                    HistoryFile[XInc].sattype, HistoryFile[XInc].eyescene,
                    HistoryFile[XInc].cloudscene, HistoryFile[XInc].eyesceneold,
                    HistoryFile[XInc].cloudsceneold, HistoryFile[XInc].rule9,
                    HistoryFile[XInc].rule8, HistoryFile[XInc].LBflag, HistoryFile[XInc].rapiddiss,
                    HistoryFile[XInc].land, HistoryFile[XInc].eyefft, HistoryFile[XInc].cloudfft,
                    HistoryFile[XInc].ringcb, HistoryFile[XInc].ringcbval,
                    HistoryFile[XInc].cwring, HistoryFile[XInc].autopos, HistoryFile[XInc].CIadjp,
                    HistoryFile[XInc].rmw, Math.max(-99.5, HistoryFile[XInc].mwscore),
                    MWDateString, HistoryFile[XInc].mwtime, HistoryFile[XInc].r34,
                    HistoryFile[XInc].MSLPenv, HistoryFile[XInc].vza, HistoryFile[XInc].comment);

            HistoryFilePtr.write(OutputString);
            XInc++;
            RecordCount++;
        }
        /* need to throw exception */
        HistoryFilePtr.close();
        return RecordCount;
    }

    public static void InitCurrent(boolean InitialFlagTF) {
        if (InitialFlagTF) {
            IRCurrentRecord.date = 1900001;
            IRCurrentRecord.time = 0;
            IRCurrentRecord.latitude = 999.5;
            IRCurrentRecord.longitude = 999.5;
            IRCurrentRecord.land = 0;
            IRCurrentRecord.autopos = 0;
            IRCurrentRecord.sattype = 0;
        }
        IRCurrentRecord.TrawO = 0.0;
        IRCurrentRecord.Traw = 0.0;
        IRCurrentRecord.Tfinal = 0.0;
        IRCurrentRecord.CI = 0.0;
        IRCurrentRecord.eyet = 99.5;
        IRCurrentRecord.cloudt = 99.5;
        IRCurrentRecord.cloudt2 = 99.5;
        IRCurrentRecord.cwcloudt = 99.5;
        IRCurrentRecord.eyecdosize = 0.0;
        IRCurrentRecord.eyestdv = 0.0;
        IRCurrentRecord.cloudsymave = 0.0;
        IRCurrentRecord.eyescene = 0;
        IRCurrentRecord.cloudscene = 0;
        IRCurrentRecord.eyesceneold = -1;
        IRCurrentRecord.cloudsceneold = -1;
        IRCurrentRecord.rule9 = 0;
        IRCurrentRecord.rule8 = 0;
        IRCurrentRecord.LBflag = 0;
        IRCurrentRecord.rapiddiss = 0;
        IRCurrentRecord.eyefft = 0;
        IRCurrentRecord.cloudfft = 0;
        IRCurrentRecord.ringcb = 0;
        IRCurrentRecord.ringcbval = 0;
        IRCurrentRecord.cwring = 0;
        IRCurrentRecord.CIadjp = 0.0;
        IRCurrentRecord.rmw = -99.5;
        IRCurrentRecord.mwscore = -99.5;
        IRCurrentRecord.mwdate = 1900001;
        IRCurrentRecord.mwtime = 0;
        IRCurrentRecord.r34 = -99;
        IRCurrentRecord.MSLPenv = -999;
        IRCurrentRecord.vza = 0;
        IRCurrentRecord.comment = "";
    }

    public void ReadHistoryFile(String filename) throws IOException {
        String delims = "[ ]+";

        File historyfile = new File(filename);
        Scanner in = new Scanner(historyfile);

        logger.debug("Opened history file {} SUCCESSFULY", filename);
        while (in.hasNextLine()) {
            HistoryFile[HistoryFileRecords] = new IRHistoryRecord();
            String historyRec = in.nextLine();
            logger.debug("Parsing History file line: " + historyRec);
            String[] tokens = historyRec.split(delims);
            /*
             * for(String histVals : tokens) { System.out.println(histVals); }
             */
            /*
             * HistoryFile[HistoryFileRecords].date =
             * Integer.parseInt(tokens[0]);
             */
            HistoryFile[HistoryFileRecords].date = Functions.cmonth2julian(tokens[0]);
            HistoryFile[HistoryFileRecords].time = Integer.parseInt(tokens[1]);
            HistoryFile[HistoryFileRecords].TrawO = Float.parseFloat(tokens[3]);
            HistoryFile[HistoryFileRecords].Traw = Float.parseFloat(tokens[4]);
            HistoryFile[HistoryFileRecords].Tfinal = Float.parseFloat(tokens[5]);
            HistoryFile[HistoryFileRecords].CI = Float.parseFloat(tokens[6]);
            HistoryFile[HistoryFileRecords].eyet = Float.parseFloat(tokens[7]);
            HistoryFile[HistoryFileRecords].cloudt = Float.parseFloat(tokens[8]);
            HistoryFile[HistoryFileRecords].cloudt2 = Float.parseFloat(tokens[9]);
            HistoryFile[HistoryFileRecords].cwcloudt = Float.parseFloat(tokens[10]);
            HistoryFile[HistoryFileRecords].latitude = Float.parseFloat(tokens[11]);
            HistoryFile[HistoryFileRecords].longitude = Float.parseFloat(tokens[12]);
            HistoryFile[HistoryFileRecords].eyecdosize = Float.parseFloat(tokens[13]);
            HistoryFile[HistoryFileRecords].eyestdv = Float.parseFloat(tokens[14]);
            HistoryFile[HistoryFileRecords].cloudsymave = Float.parseFloat(tokens[15]);
            HistoryFile[HistoryFileRecords].sattype = Integer.parseInt(tokens[16]);
            HistoryFile[HistoryFileRecords].eyescene = Integer.parseInt(tokens[17]);
            HistoryFile[HistoryFileRecords].cloudscene = Integer.parseInt(tokens[18]);
            HistoryFile[HistoryFileRecords].eyesceneold = Integer.parseInt(tokens[19]);
            HistoryFile[HistoryFileRecords].cloudsceneold = Integer.parseInt(tokens[20]);
            HistoryFile[HistoryFileRecords].rule9 = Integer.parseInt(tokens[21]);
            HistoryFile[HistoryFileRecords].rule8 = Integer.parseInt(tokens[22]);
            HistoryFile[HistoryFileRecords].LBflag = Integer.parseInt(tokens[23]);
            HistoryFile[HistoryFileRecords].rapiddiss = Integer.parseInt(tokens[24]);
            HistoryFile[HistoryFileRecords].land = Integer.parseInt(tokens[25]);
            HistoryFile[HistoryFileRecords].eyefft = Integer.parseInt(tokens[26]);
            HistoryFile[HistoryFileRecords].cloudfft = Integer.parseInt(tokens[27]);
            HistoryFile[HistoryFileRecords].ringcb = Integer.parseInt(tokens[28]);
            HistoryFile[HistoryFileRecords].ringcbval = Integer.parseInt(tokens[29]);
            HistoryFile[HistoryFileRecords].cwring = Integer.parseInt(tokens[30]);
            HistoryFile[HistoryFileRecords].autopos = Integer.parseInt(tokens[31]);
            HistoryFile[HistoryFileRecords].CIadjp = Float.parseFloat(tokens[32]);
            HistoryFile[HistoryFileRecords].rmw = Float.parseFloat(tokens[33]);
            HistoryFile[HistoryFileRecords].mwscore = Float.parseFloat(tokens[34]);
            HistoryFile[HistoryFileRecords].mwdate = Functions.cmonth2julian(tokens[35]);
            HistoryFile[HistoryFileRecords].mwtime = Integer.parseInt(tokens[36]);
            HistoryFile[HistoryFileRecords].r34 = Integer.parseInt(tokens[37]);
            HistoryFile[HistoryFileRecords].MSLPenv = Integer.parseInt(tokens[38]);
            HistoryFile[HistoryFileRecords].vza = Integer.parseInt(tokens[39]);
            if (tokens.length > 40) {
                if (tokens[40] != null) {
                    /* this won't get all comment past first space */
                    HistoryFile[HistoryFileRecords].comment = tokens[40];
                }
            } else {
                HistoryFile[HistoryFileRecords].comment = "";
            }
            HistoryFileRecords++;
        }
        in.close();
        logger.debug("Done reading History file, number of records: " + HistoryFileRecords);
    }
}