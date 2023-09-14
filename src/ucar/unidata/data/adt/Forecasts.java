/*
 * $Id: CacheDataSource.java,v 1.12 2007/08/17 20:34:15 jeffmc Exp $
 *
 * Copyright 1997-2023 Unidata Program Center/University Corporation for
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
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Forecasts {

    private static final Logger logger = LoggerFactory.getLogger(Forecasts.class);

    private static double ForecastLatitudes[] = new double[10];

    private static double ForecastLongitudes[] = new double[10];

    private static double ForecastTimes[] = new double[10];

    private static String[] MonthID_StringX = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL",
            "AUG", "SEP", "OCT", "NOV", "DEC" };

    public Forecasts() {
    }

    public static double[] ReadForecasts(String ForecastFileName, int ForecastFileType,
                                         double ThresholdTime) throws IOException {
        String delims = "[, ]+";

        double ReturnLatitudeInterpolation = -999.5;
        double ReturnLongitudeInterpolation = -999.5;
        double ReturnCurrentIntensity = -999.5;
        double CurrentDayValue;
        int ReturnFlagValue = -1;
        String TmpStr1;
        String TmpStr2;
        String TmpStr3;
        String TmpStr4;
        String TmpStr5;
        String TmpStr6;
        String TmpStr7;
        String TmpStr8;
        String TmpStr9;
        String TmpStr12;
        int WindVal;
        int YearIntVal;
        int MonthIntVal;
        int DayIntVal;
        int HMSTimeIntVal;
        int FcstTimeIntVal;
        int JulianDayIntVal;
        int FcstTimeIntValPrev;
        int LocVar1, LocVar2;
        int DayIntValXX;
        int MonthIntValXX;
        int YearIntValXX;
        int JulianDayIntValXX;
        double TimeArray[] = new double[10];
        double LatitudeArray[] = new double[10];
        double LongitudeArray[] = new double[10];
        double StrengthArray[] = new double[10];
        double TmpTimeVal2;
        double NSFactor;
        double WEFactor;

        ReturnFlagValue = 0;

        logger.debug("ReadForecasts in...");
        System.out.printf("Forecast File Name=%s Type=%d\n", ForecastFileName, ForecastFileType);
        File forecastfile = new File(ForecastFileName);
        Scanner in = new Scanner(forecastfile);

        int ImageDate = Data.IRData_JulianDate;
        int ImageTime = Data.IRData_HHMMSSTime;
        System.out.printf("IMAGE DATE=%d  TIME=%d\n", Data.IRData_JulianDate,
                Data.IRData_HHMMSSTime);
        /* int ImageDate = History.IRCurrentRecord.date; */
        /* int ImageTime = History.IRCurrentRecord.time; */
        double CurrentTime = Functions.calctime(ImageDate, ImageTime);
        int XInc = 0;
        int YInc = 0;
        FcstTimeIntValPrev = -1;

        try {

            /* char aChar = ATCFStormIDString.charAt(2); */
            if (ForecastFileType == 0) {
                while (in.hasNextLine()) {
                    String forecastRec = in.nextLine();
                    String[] tokens = forecastRec.split(delims);
                    TmpStr1 = tokens[0];
                    TmpStr2 = tokens[1];
                    TmpStr3 = tokens[2];
                    TmpStr4 = tokens[3];
                    TmpStr5 = tokens[4];
                    TmpStr6 = tokens[5];
                    TmpStr7 = tokens[6];
                    TmpStr8 = tokens[7];
                    TmpStr9 = tokens[8];
                    TmpStr12 = tokens[11];
                    WindVal = 0;
                    if ((TmpStr12.length() > 1) && (TmpStr12.matches("\\d+"))) {
                        WindVal = Integer.parseInt(TmpStr12.substring(0, 2)); /*
                         * (
                         * int
                         * )
                         * aodt_atoif
                         * (
                         * TmpStr12
                         * ,
                         * 1
                         * ,
                         * 2
                         * )
                         * ;
                         */
                    }
                    /*
                     * System.out.printf(
                     * "1=%s* 2=%s* 3=%s* 4=%s* 5=%s* 6=%s* 7=%s* 8=%s* 9=%s* 10=%s* 11=%s* 12=%s* \n"
                     * ,
                     * TmpStr1,TmpStr2,TmpStr3,TmpStr4,TmpStr5,TmpStr6,TmpStr7,
                     * TmpStr8,TmpStr9,TmpStr10,TmpStr11,TmpStr12);
                     */
                    if (WindVal == 34) {
                        YearIntVal = Integer.parseInt(TmpStr3.substring(0, 4)); /*
                         * (
                         * int
                         * )
                         * aodt_atoif
                         * (
                         * TmpStr3
                         * ,
                         * 1
                         * ,
                         * 4
                         * )
                         * ;
                         */
                        MonthIntVal = Integer.parseInt(TmpStr3.substring(4, 6)); /*
                         * (
                         * int
                         * )
                         * aodt_atoif
                         * (
                         * TmpStr3
                         * ,
                         * 5
                         * ,
                         * 6
                         * )
                         * ;
                         */
                        DayIntVal = Integer.parseInt(TmpStr3.substring(6, 8)); /*
                         * (
                         * int
                         * )
                         * aodt_atoif
                         * (
                         * TmpStr3
                         * ,
                         * 7
                         * ,
                         * 8
                         * )
                         * ;
                         */
                        /*
                         * System.out.printf("year=%d month=%d day=%d\n",YearIntVal
                         * ,MonthIntVal,DayIntVal);
                         */
                        HMSTimeIntVal = 10000 * Integer.parseInt(TmpStr3.substring(8, 10)); /*
                         * 10000
                         * *
                         * (
                         * (
                         * int
                         * )
                         * aodt_atoif
                         * (
                         * TmpStr3
                         * ,
                         * 9
                         * ,
                         * 10
                         * )
                         * )
                         * ;
                         */
                        FcstTimeIntVal = 10000 * Integer.parseInt(TmpStr6); /*
                         * 10000
                         * *
                         * (
                         * (
                         * int
                         * )
                         * aodt_atoif
                         * (
                         * TmpStr6
                         * ,
                         * 1
                         * ,
                         * strlen
                         * (
                         * TmpStr6
                         * )
                         * )
                         * )
                         * ;
                         */
                        JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal, YearIntVal);
                        TmpTimeVal2 = Functions.calctime(JulianDayIntVal, HMSTimeIntVal);
                        /*
                         * System.out.printf("YInc=%d tmptime=%f\n",YInc,TmpTimeVal2
                         * );
                         */
                        if ((TmpTimeVal2 < CurrentTime) && (FcstTimeIntValPrev != FcstTimeIntVal)
                                && (YInc < 6)) {
                            LocVar1 = ((int) (FcstTimeIntVal + HMSTimeIntVal)) / 240000;
                            LocVar2 = (((int) (FcstTimeIntVal + HMSTimeIntVal)) % 240000);
                            TimeArray[YInc] = Functions
                                    .calctime(JulianDayIntVal + LocVar1, LocVar2);
                            NSFactor = (TmpStr7.charAt(TmpStr7.length() - 1) == 'N') ? 1.0 : -1.0;
                            WEFactor = (TmpStr8.charAt(TmpStr8.length() - 1) == 'W') ? 1.0 : -1.0;
                            LatitudeArray[YInc] = NSFactor
                                    * Double.parseDouble(TmpStr7.substring(0, TmpStr7.length() - 1))
                                    / 10.0; /*
                             * (aodt_atoif(TmpStr7,1,strlen(TmpStr7
                             * )))/10.0;
                             */
                            LongitudeArray[YInc] = WEFactor
                                    * Double.parseDouble(TmpStr8.substring(0, TmpStr8.length() - 1))
                                    / 10.0; /*
                             * (aodt_atoif(TmpStr8,1,strlen(TmpStr8
                             * )))/10.0;
                             */
                            StrengthArray[YInc] = Double.parseDouble(TmpStr9); /*
                             * aodt_atoif
                             * (
                             * TmpStr9
                             * ,
                             * 1
                             * ,
                             * 4
                             * )
                             * ;
                             */
                            YInc++;
                            FcstTimeIntValPrev = FcstTimeIntVal;
                            XInc = YInc;
                        }
                    }
                }
            } else if (ForecastFileType == 1) { /* NHC */
                /* read NHC forecast file, quit at EOF */
                DayIntValXX = 0;
                MonthIntValXX = 0;
                YearIntValXX = 0;
                JulianDayIntValXX = 0;
                DayIntVal = 0;
                MonthIntVal = 0;
                YearIntVal = 0;
                JulianDayIntVal = 0;
                boolean NewNHCFormatFlag = false;
                boolean FoundMonthTF = false;
                boolean FoundForecast = false;
                while ((in.hasNextLine()) && (XInc < 6)) {
                    String forecastRec = in.nextLine();
                    String[] tokens = forecastRec.split(delims);
                    if (!FoundMonthTF) {
                        if ((tokens.length == 7)
                                && (tokens[1].equals("AM") || tokens[1].equals("PM"))) {
                            /*
                             * this is reading the header at the top of the
                             * forecast file. It is done to obtain the current
                             * month and year of the forecast so that we can
                             * check it accurately against the image date/time
                             */
                            DayIntValXX = Integer.parseInt(tokens[5].substring(0, 2)); /*
                             * (
                             * int
                             * )
                             * aodt_atoif
                             * (
                             * TmpStr6
                             * ,
                             * 1
                             * ,
                             * 2
                             * )
                             * ;
                             */
                            YearIntValXX = Integer.parseInt(tokens[6].substring(0, 4)); /*
                             * (
                             * int
                             * )
                             * aodt_atoif
                             * (
                             * TmpStr7
                             * ,
                             * 1
                             * ,
                             * 4
                             * )
                             * ;
                             */
                            while ((!tokens[4].equals(MonthID_StringX[MonthIntValXX]))
                                    && (MonthIntValXX < 12)) {
                                MonthIntValXX++;
                            }
                            JulianDayIntValXX = Functions.idmyyd(DayIntValXX, MonthIntValXX + 1,
                                    YearIntValXX);
                            FoundMonthTF = true;
                            /*
                             * System.out.printf(
                             * "XX julday=%d day=%d month=%d year=%d\n"
                             * ,JulianDayIntValXX
                             * ,DayIntValXX,MonthIntValXX+1,YearIntValXX);
                             */
                        }
                        MonthIntVal = MonthIntValXX;
                        YearIntVal = YearIntValXX;
                    } else {
                        if ((FoundForecast) && (tokens.length >= 6)) {
                            /* System.out.printf("length=%d\n",tokens.length); */
                            if (XInc == 0) {
                                if (tokens[0].equals("INITIAL")) {
                                    NewNHCFormatFlag = false;
                                } else if ((tokens[0].equals("INIT")) && (YearIntVal >= 2011)) {
                                    NewNHCFormatFlag = true;
                                } else {
                                    NewNHCFormatFlag = false;
                                }
                                /*
                                 * System.out.printf("newformatflag=%b\n",
                                 * NewNHCFormatFlag);
                                 */
                            }
                            int iadd = (tokens[0].length() == 0) ? 1 : 0;
                            if ((NewNHCFormatFlag) || ((!NewNHCFormatFlag) && (XInc == 0))) {
                                TmpStr1 = tokens[0 + iadd];
                                TmpStr2 = tokens[1 + iadd];
                                TmpStr3 = tokens[2 + iadd];
                                TmpStr4 = tokens[3 + iadd];
                                TmpStr5 = tokens[4 + iadd];
                                TmpStr6 = tokens[5 + iadd];
                            } else {
                                TmpStr1 = tokens[0 + iadd];
                                TmpStr2 = tokens[2 + iadd];
                                TmpStr3 = tokens[3 + iadd];
                                TmpStr4 = tokens[4 + iadd];
                                TmpStr5 = tokens[5 + iadd];
                                TmpStr6 = tokens[6 + iadd];
                            }
                            /*
                             * System.out.printf(
                             * "1=%s* 1a=%s* 2=%s* 3=%s* 4=%s* 5=%s* 6=%s*\n"
                             * ,TmpStr1
                             * ,TmpStr1a,TmpStr2,TmpStr3,TmpStr4,TmpStr5,
                             * TmpStr6);
                             */
                            if (TmpStr6.substring(0, 2).equals("KT")) {
                                if (TmpStr2.charAt(7) == 'Z') {
                                    DayIntVal = Integer.parseInt(TmpStr2.substring(0, 2)); /*
                                     * (
                                     * (
                                     * int
                                     * )
                                     * aodt_atoif
                                     * (
                                     * TmpStr2
                                     * ,
                                     * 1
                                     * ,
                                     * 7
                                     * )
                                     * )
                                     * /
                                     * 10000
                                     * ;
                                     */
                                    HMSTimeIntVal = 100 * (Integer
                                            .parseInt(TmpStr2.substring(3, 7))); /*
                                     * 100
                                     * *
                                     * (
                                     * (
                                     * (
                                     * int
                                     * )
                                     * aodt_atoif
                                     * (
                                     * TmpStr2
                                     * ,
                                     * 1
                                     * ,
                                     * 7
                                     * )
                                     * )
                                     * %
                                     * 10000
                                     * )
                                     * ;
                                     */
                                    if (DayIntVal < DayIntValXX) {
                                        MonthIntVal++;
                                        if (MonthIntVal == 12) {
                                            YearIntVal++;
                                            MonthIntVal = 0;
                                        }
                                        JulianDayIntVal = Functions.idmyyd(DayIntVal,
                                                MonthIntVal + 1, YearIntVal);
                                    } else {
                                        JulianDayIntVal = JulianDayIntValXX
                                                + (DayIntVal - DayIntValXX);
                                    }
                                    TimeArray[XInc] = Functions.calctime(JulianDayIntVal,
                                            HMSTimeIntVal);
                                    NSFactor = (TmpStr3.charAt(TmpStr3.length() - 1) == 'N') ? 1.0
                                            : -1.0;
                                    WEFactor = (TmpStr4.charAt(TmpStr4.length() - 1) == 'W') ? 1.0
                                            : -1.0;
                                    LatitudeArray[XInc] = NSFactor
                                            * Double.parseDouble(TmpStr3.substring(0,
                                            TmpStr3.length() - 1));
                                    LongitudeArray[XInc] = WEFactor
                                            * Double.parseDouble(TmpStr4.substring(0,
                                            TmpStr4.length() - 1));
                                    StrengthArray[XInc] = Integer.parseInt(TmpStr5);
                                    XInc++;
                                }
                            }
                        } else {
                            if (tokens[0].equals("FORECAST") && tokens[1].equals("POSITIONS"))
                                FoundForecast = true;
                        }
                    }
                }
            } else if (ForecastFileType == 2) {
                /*
                 * read JTWC forecast file, quit at EOF
                 *
                 * o.k... since JTWC does not put the month and year on their
                 * bulletins, they will need to be "made up". We will assume
                 * that the forecast is current and the month and year from the
                 * image is equal to the month and year of the forecast file.
                 */
                int[] ReturnValues = Functions.adt_yddmy((int) CurrentTime);
                DayIntValXX = ReturnValues[0];
                MonthIntValXX = ReturnValues[1];
                YearIntValXX = ReturnValues[2];
                /* System.out.printf("DayIntValXX=%d\n",DayIntValXX); */
                /* System.out.printf("MonthIntValXX=%d\n",MonthIntValXX); */
                /* System.out.printf("YearIntValXX=%d\n",YearIntValXX); */
                int datexxx = 0;
                int monthxxx = 0;
                int yearxxx = 0;
                int DayIntValSub = 0;
                JulianDayIntVal = 0;
                while (in.hasNextLine()) {
                    String forecastRec = in.nextLine();
                    String[] tokens = forecastRec.split(delims);
                    if (tokens.length >= 5) {
                        int iadd = (tokens[0].length() == 0) ? 1 : 0;
                        TmpStr1 = tokens[0 + iadd];
                        TmpStr2 = tokens[1 + iadd];
                        TmpStr3 = tokens[2 + iadd];
                        TmpStr4 = tokens[3 + iadd];
                        if (TmpStr2.equals("---")) {
                            if (XInc == 0) {
                                TmpStr5 = tokens[4 + iadd];
                                /*
                                 * System.out.printf(
                                 * "XInc=%d : 1=%s* 2=%s* 3=%s* 4=%s* 5=%s* \n"
                                 * ,XInc
                                 * ,TmpStr1,TmpStr2,TmpStr3,TmpStr4,TmpStr5);
                                 */
                                DayIntVal = Integer.parseInt(TmpStr1.substring(0, 2));
                                HMSTimeIntVal = 100 * Integer.parseInt(TmpStr1.substring(2, 6));
                                /*
                                 * System.out.printf(
                                 * "dayintval=%d hmstimeintval=%d\n"
                                 * ,DayIntVal,HMSTimeIntVal);
                                 */
                                DayIntValSub = DayIntValXX - DayIntVal;
                                int[] ReturnValues2 = Functions.adt_yddmy(ImageDate);
                                datexxx = ReturnValues2[0];
                                monthxxx = ReturnValues2[1];
                                yearxxx = ReturnValues2[2];
                                if (DayIntVal == DayIntValXX) {
                                    /*
                                     * dates are the same... probably o.k....
                                     * should check times
                                     */
                                    JulianDayIntVal = (int) CurrentTime;
                                } else if ((DayIntValSub <= 2) && (DayIntValSub >= 0)) {
                                    /*
                                     * this is probably o.k too... should check
                                     * times
                                     */
                                    JulianDayIntVal = (int) CurrentTime + (DayIntVal - DayIntValXX);
                                } else {
                                    /*
                                     * dates are invalid. Either image date is
                                     * before forecast or is well beyond
                                     * forecast availability
                                     */
                                    if (DayIntValSub < -27) {
                                        /*
                                         * System.out.printf("month crossing\n");
                                         */
                                        /*
                                         * System.out.printf("datexxx=%d\n",datexxx
                                         * );
                                         */
                                        if ((datexxx <= 2) && (monthxxx == 1)) {
                                            /*
                                             * System.out.printf(
                                             * "year crossing... image during new year: %d %d\n"
                                             * ,yearxxx-1,DayIntVal);
                                             */
                                            JulianDayIntVal = Functions.idmyyd(DayIntVal, 12,
                                                    yearxxx - 1);
                                        } else {
                                            JulianDayIntVal = Functions.idmyyd(DayIntVal,
                                                    monthxxx - 1, yearxxx);
                                        }
                                        /*
                                         * System.out.printf("JulianDayIntVal=%d\n"
                                         * ,JulianDayIntVal);
                                         */
                                    } else {
                                        ReturnFlagValue = -1;
                                    }
                                }
                                /*
                                 * System.out.printf("JulianDayIntVal=%d\n",
                                 * JulianDayIntVal);
                                 */
                                TimeArray[XInc] = Functions
                                        .calctime(JulianDayIntVal, HMSTimeIntVal);
                                /*
                                 * System.out.printf("TimeArray[%d]=%f\n",XInc,
                                 * TimeArray[XInc]);
                                 */
                                NSFactor = (TmpStr4.charAt(TmpStr4.length() - 1) == 'N') ? 1.0
                                        : -1.0;
                                WEFactor = (TmpStr5.charAt(TmpStr5.length() - 1) == 'W') ? 1.0
                                        : -1.0;
                                LatitudeArray[XInc] = NSFactor
                                        * Double.parseDouble(TmpStr4.substring(0,
                                        TmpStr4.length() - 1));
                                LongitudeArray[XInc] = WEFactor
                                        * Double.parseDouble(TmpStr5.substring(0,
                                        TmpStr5.length() - 1));
                            } else {
                                /*
                                 * System.out.printf(
                                 * "XInc=%d : 1=%s* 2=%s* 3=%s* 4=%s* \n"
                                 * ,XInc,TmpStr1,TmpStr2,TmpStr3,TmpStr4);
                                 */
                                DayIntVal = Integer.parseInt(TmpStr1.substring(0, 2));
                                HMSTimeIntVal = 100 * Integer.parseInt(TmpStr1.substring(2, 6));
                                /*
                                 * System.out.printf(
                                 * "dayintval=%d hmstimeintval=%d\n"
                                 * ,DayIntVal,HMSTimeIntVal);
                                 */
                                DayIntValSub = DayIntValXX - DayIntVal;
                                if (DayIntValSub < -27) {
                                    if ((monthxxx - 1) == 0) {
                                        JulianDayIntVal = Functions.idmyyd(DayIntVal, 12,
                                                yearxxx - 1);
                                    } else {
                                        JulianDayIntVal = Functions.idmyyd(DayIntVal, monthxxx - 1,
                                                yearxxx);
                                    }
                                } else {
                                    if (DayIntValSub > 27) {
                                        if ((monthxxx + 1) > 12) {
                                            JulianDayIntVal = Functions.idmyyd(DayIntVal, 1,
                                                    yearxxx + 1);
                                        } else {
                                            JulianDayIntVal = Functions.idmyyd(DayIntVal,
                                                    monthxxx + 1, yearxxx);
                                        }
                                    } else {
                                        JulianDayIntVal = Functions.idmyyd(DayIntVal, monthxxx,
                                                yearxxx);
                                    }
                                }
                                /*
                                 * System.out.printf("JulianDayIntVal=%d\n",
                                 * JulianDayIntVal);
                                 */
                                TimeArray[XInc] = Functions
                                        .calctime(JulianDayIntVal, HMSTimeIntVal);
                                /*
                                 * System.out.printf("TimeArray[%d]=%f\n",XInc,
                                 * TimeArray[XInc]);
                                 */
                                NSFactor = (TmpStr3.charAt(TmpStr3.length() - 1) == 'N') ? 1.0
                                        : -1.0;
                                WEFactor = (TmpStr4.charAt(TmpStr4.length() - 1) == 'W') ? 1.0
                                        : -1.0;
                                LatitudeArray[XInc] = NSFactor
                                        * Double.parseDouble(TmpStr3.substring(0,
                                        TmpStr3.length() - 1));
                                LongitudeArray[XInc] = WEFactor
                                        * Double.parseDouble(TmpStr4.substring(0,
                                        TmpStr4.length() - 1));
                            }
                        }
                        if ((TmpStr1.equals("MAX")) && (TmpStr4.charAt(0) == '-')) {
                            TmpStr5 = tokens[4 + iadd];
                            if (XInc == 0) {
                                StrengthArray[XInc] = Double.parseDouble(TmpStr5);
                            }
                            XInc++;
                        }
                    }
                }
            } else if (ForecastFileType == 3) {
                /* generic forecast file input */
                while (in.hasNextLine()) {
                    String forecastRec = in.nextLine();
                    String[] tokens = forecastRec.split(delims);
                    if (tokens.length >= 5) {
                        TmpStr1 = tokens[0];
                        TmpStr2 = tokens[1];
                        TmpStr3 = tokens[2];
                        TmpStr4 = tokens[3];
                        TmpStr5 = tokens[4];
                        TmpStr6 = tokens[5];
                        TmpStr7 = tokens[6];
                        DayIntVal = Integer.parseInt(TmpStr1.substring(0, 2));
                        MonthIntVal = Integer.parseInt(TmpStr2.substring(0, 2));
                        YearIntVal = Integer.parseInt(TmpStr3.substring(0, 4));
                        HMSTimeIntVal = 100 * Integer.parseInt(TmpStr4.substring(0, 4));
                        JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal, YearIntVal);
                        TimeArray[XInc] = Functions.calctime(JulianDayIntVal, HMSTimeIntVal);
                        NSFactor = (TmpStr5.charAt(TmpStr5.length() - 1) == 'N') ? 1.0 : -1.0;
                        WEFactor = (TmpStr6.charAt(TmpStr6.length() - 1) == 'W') ? 1.0 : -1.0;
                        LatitudeArray[XInc] = NSFactor
                                * Double.parseDouble(TmpStr5.substring(0, TmpStr5.length() - 1));
                        LongitudeArray[XInc] = WEFactor
                                * Double.parseDouble(TmpStr6.substring(0, TmpStr6.length() - 1));
                        StrengthArray[XInc] = Double.parseDouble(TmpStr5);
                        XInc++;
                    }
                }
            } else if (ForecastFileType == 4) {
                String delims4 = "[:+/ ]+";
                /* RSMC ICAO (FK?? or WT??) forecast file input */
                /*
                 * Convert the ICAO RSMC bulletin positions from degrees minutes
                 * to degrees and tenths of degrees. fix courtesy : YanLuo/NOAA
                 * 07/22/09 *
                 */
                int xxx1 = 0;
                int xxx2 = 0;
                double LatMultFact = 0.0;
                double LonMultFact = 0.0;
                while (in.hasNextLine()) {
                    String forecastRec = in.nextLine();
                    String[] tokens = forecastRec.split(delims4);
                    if (tokens[0].equals("DTG")) {
                        TmpStr1 = tokens[1];
                        /* System.out.printf("TmpStr1=%s*\n",TmpStr1); */
                        DayIntVal = Integer.parseInt(TmpStr1.substring(6, 8));
                        MonthIntVal = Integer.parseInt(TmpStr1.substring(4, 6));
                        YearIntVal = Integer.parseInt(TmpStr1.substring(0, 4));
                        if (tokens.length == 2) {
                            HMSTimeIntVal = 100 * Integer.parseInt(TmpStr1.substring(8, 12));
                        } else {
                            TmpStr2 = tokens[2];
                            HMSTimeIntVal = 100 * Integer.parseInt(TmpStr2.substring(0, 4));
                        }
                        JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal, YearIntVal);
                        TimeArray[XInc] = Functions.calctime(JulianDayIntVal, HMSTimeIntVal);
                        /*
                         * System.out.printf("time0=%f  hmstime=%d\n",TimeArray[XInc
                         * ],HMSTimeIntVal);
                         */
                    } else if (tokens[0].equals("PSN")) {
                        TmpStr1 = tokens[1];
                        TmpStr2 = tokens[2];
                        LatMultFact = (TmpStr1.charAt(0) == 'N') ? 1.0 : -1.0;
                        LonMultFact = (TmpStr2.charAt(0) == 'W') ? 1.0 : -1.0;
                        xxx1 = Integer.parseInt(TmpStr1.substring(1, TmpStr1.length()));
                        xxx2 = Integer.parseInt(TmpStr2.substring(1, TmpStr2.length()));
                        /* initial lat/lon */
                        double LatitudeWhole = ((double) xxx1) / 100.0;
                        double LatitudePart = ((double) xxx1 - (LatitudeWhole * 100.0)) / 60.0;
                        LatitudeArray[XInc] = LatMultFact * (LatitudeWhole + LatitudePart);
                        double LongitudeWhole = ((double) xxx2) / 100.0;
                        double LongitudePart = ((double) xxx2 - (LongitudeWhole * 100.0)) / 60.0;
                        LongitudeArray[XInc] = LonMultFact * (LongitudeWhole + LongitudePart);
                        XInc++;
                    } else if ((tokens[0].equals("MAX")) && (XInc == 0)) {
                        /* initial lat/lon */
                        TmpStr1 = tokens[2];
                        StrengthArray[XInc] = Double.parseDouble(TmpStr1.substring(0,
                                TmpStr1.length() - 2));
                    } else if ((tokens[0].equals("FCST")) && (tokens[1].equals("PSN"))) {
                        int numtokens = tokens.length;
                        TmpStr1 = tokens[2];
                        TmpStr2 = tokens[numtokens - 2];
                        TmpStr3 = tokens[numtokens - 1];
                        /*
                         * System.out.printf("1=%s* 2=%s* 3=%s*\n",TmpStr1,TmpStr2
                         * ,TmpStr3);
                         */
                        LatMultFact = (TmpStr2.charAt(0) == 'N') ? 1.0 : -1.0;
                        LonMultFact = (TmpStr3.charAt(0) == 'W') ? 1.0 : -1.0;
                        xxx1 = Integer.parseInt(TmpStr2.substring(1, TmpStr2.length()));
                        xxx2 = Integer.parseInt(TmpStr3.substring(1, TmpStr3.length()));
                        int FcstHrAddIntVal = Integer.parseInt(TmpStr1.substring(0, 2));
                        /* initial lat/lon */
                        double LatitudeWhole = ((double) xxx1) / 100.0;
                        double LatitudePart = ((double) xxx1 - (LatitudeWhole * 100.0)) / 60.0;
                        LatitudeArray[XInc] = LatMultFact * (LatitudeWhole + LatitudePart);
                        double LongitudeWhole = ((double) xxx2) / 100.0;
                        double LongitudePart = ((double) xxx2 - (LongitudeWhole * 100.0)) / 60.0;
                        LongitudeArray[XInc] = LonMultFact * (LongitudeWhole + LongitudePart);
                        double FcstHrAddDblVal = ((double) FcstHrAddIntVal) / 24.0;
                        /*
                         * System.out.printf("XInc=%d time0=%f fcsthr=%f\n",XInc,
                         * TimeArray[0],FcstHrAddDblVal);
                         */
                        TimeArray[XInc] = TimeArray[0] + FcstHrAddDblVal;
                        XInc++;
                    }
                }
            } else if (ForecastFileType == 5) {
                /* RSMC WTIO30 forecast file input */
                String delims5 = "[,:/= ]+";
                while (in.hasNextLine()) {
                    String forecastRec = in.nextLine();
                    String[] tokens = forecastRec.split(delims5);
                    if (tokens[0].equals("2.A")) {
                        TmpStr1 = tokens[2];
                        TmpStr2 = tokens[3];
                        TmpStr3 = tokens[4];
                        TmpStr4 = tokens[6];
                        YearIntVal = Integer.parseInt(TmpStr1);
                        MonthIntVal = Integer.parseInt(TmpStr2);
                        DayIntVal = Integer.parseInt(TmpStr3);
                        HMSTimeIntVal = 100 * Integer.parseInt(TmpStr4);
                        JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal, YearIntVal);
                        TimeArray[XInc] = Functions.calctime(JulianDayIntVal, HMSTimeIntVal);
                        forecastRec = in.nextLine();
                        tokens = forecastRec.split(delims5);
                        if (tokens.length == 2) {
                            TmpStr1 = tokens[0];
                            TmpStr2 = tokens[1];
                        } else {
                            /*
                             * expecting line like WITHIN 20 NM RADIUS OF POINT
                             * 22.2S / 50.8E
                             */
                            TmpStr1 = tokens[6];
                            TmpStr2 = tokens[7];
                        }
                        NSFactor = (TmpStr1.charAt(TmpStr1.length() - 1) == 'N') ? 1.0 : -1.0;
                        WEFactor = (TmpStr2.charAt(TmpStr2.length() - 1) == 'W') ? 1.0 : -1.0;
                        LatitudeArray[XInc] = NSFactor
                                * Double.parseDouble(TmpStr1.substring(0, TmpStr1.length() - 1));
                        LongitudeArray[XInc] = WEFactor
                                * Double.parseDouble(TmpStr2.substring(0, TmpStr2.length() - 1));
                    } else if (tokens[0].equals("5.A")) {
                        TmpStr1 = tokens[tokens.length - 2];
                        StrengthArray[XInc] = Double.parseDouble(TmpStr1);
                    } else if (tokens[0].equals("1.B")) {
                        XInc = 1;
                        forecastRec = in.nextLine();
                        tokens = forecastRec.split(delims5);
                        boolean ICAOValidFcstTF = true;
                        while ((ICAOValidFcstTF) && (XInc < 6)) {
                            TmpStr1 = tokens[1];
                            TmpStr2 = tokens[2];
                            TmpStr3 = tokens[3];
                            TmpStr4 = tokens[4];
                            TmpStr5 = tokens[6];
                            TmpStr6 = tokens[7];
                            TmpStr7 = tokens[10];
                            YearIntVal = Integer.parseInt(TmpStr1);
                            MonthIntVal = Integer.parseInt(TmpStr2);
                            DayIntVal = Integer.parseInt(TmpStr3);
                            HMSTimeIntVal = 10000 * Integer.parseInt(TmpStr4);
                            JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal, YearIntVal);
                            TimeArray[XInc] = Functions.calctime(JulianDayIntVal, HMSTimeIntVal);
                            NSFactor = (TmpStr5.charAt(TmpStr5.length() - 1) == 'N') ? 1.0 : -1.0;
                            WEFactor = (TmpStr6.charAt(TmpStr6.length() - 1) == 'W') ? 1.0 : -1.0;
                            LatitudeArray[XInc] = NSFactor
                                    * Double.parseDouble(TmpStr5.substring(0, TmpStr5.length() - 1));
                            LongitudeArray[XInc] = WEFactor
                                    * Double.parseDouble(TmpStr6.substring(0, TmpStr6.length() - 1));
                            StrengthArray[XInc] = (double) Integer
                                    .parseInt(TmpStr7.substring(0, 3));
                            forecastRec = in.nextLine();
                            tokens = forecastRec.split(delims5);
                            String TmpStr0 = tokens[0];
                            if (tokens[0].equals("2.C")) {
                                ICAOValidFcstTF = false;
                            } else {
                                if (TmpStr0.charAt(2) != 'H') {
                                    forecastRec = in.nextLine();
                                    ICAOValidFcstTF = false;
                                }
                            }
                            XInc++;
                        }
                    }
                }
            } else if (ForecastFileType == 6) {
                /* TCWC AXAU Tech Bulletins file input */
                String delims6 = "[,:/ ]+";
                JulianDayIntVal = 0;
                MonthIntVal = 0;
                YearIntVal = 0;
                while (in.hasNextLine()) {
                    String forecastRec = in.nextLine();
                    String[] tokens = forecastRec.split(delims6);
                    if (tokens[0].equals("at")) {
                        /* initial date */
                        TmpStr1 = tokens[1];
                        TmpStr2 = tokens[3];
                        TmpStr3 = tokens[4];
                        TmpStr4 = tokens[5];
                        YearIntVal = Integer.parseInt(TmpStr4);
                        MonthIntVal = Integer.parseInt(TmpStr3);
                        DayIntVal = Integer.parseInt(TmpStr2);
                        /* HMSTimeIntVal = 100*Integer.parseInt(TmpStr1); */
                        JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal, YearIntVal);
                    }
                    if (tokens[0].equals("Data")) {
                        /* initial time */
                        TmpStr1 = tokens[2];
                        HMSTimeIntVal = 100 * Integer.parseInt(TmpStr1);
                        TimeArray[XInc] = Functions.calctime(JulianDayIntVal, HMSTimeIntVal);
                    }
                    if (tokens[0].equals("Latitude")) {
                        /* initial latitude */
                        TmpStr1 = tokens[1];
                        NSFactor = (TmpStr1.charAt(TmpStr1.length() - 1) == 'N') ? 1.0 : -1.0;
                        LatitudeArray[XInc] = NSFactor
                                * Double.parseDouble(TmpStr1.substring(0, TmpStr1.length() - 1));
                    }
                    if (tokens[0].equals("Longitude")) {
                        /* initial longitude */
                        TmpStr1 = tokens[1];
                        WEFactor = (TmpStr1.charAt(TmpStr1.length() - 1) == 'W') ? 1.0 : -1.0;
                        LongitudeArray[XInc] = WEFactor
                                * Double.parseDouble(TmpStr1.substring(0, TmpStr1.length() - 1));
                    }
                    if (tokens[0].equals("Maximum")) {
                        if (tokens[1].equals("10-Minute")) {
                            /* initial strength */
                            TmpStr1 = tokens[3];
                            StrengthArray[XInc] = (double) Integer.parseInt(TmpStr1);
                        }
                    }
                    if (tokens[0].equals("FORECAST")) {
                        XInc = 1;
                        forecastRec = in.nextLine();
                        forecastRec = in.nextLine();
                        forecastRec = in.nextLine();
                        tokens = forecastRec.split(delims6);
                        boolean ICAOValidFcstTF = true;
                        while ((ICAOValidFcstTF) && (XInc < 6)) {
                            TmpStr1 = tokens[1];
                            TmpStr2 = tokens[2];
                            TmpStr3 = tokens[3];
                            TmpStr4 = tokens[4];
                            TmpStr5 = tokens[7];
                            DayIntVal = Integer.parseInt(TmpStr1);
                            HMSTimeIntVal = 100 * Integer.parseInt(TmpStr2);
                            JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal, YearIntVal);
                            TimeArray[XInc] = Functions.calctime(JulianDayIntVal, HMSTimeIntVal);
                            NSFactor = (TmpStr3.charAt(TmpStr3.length() - 1) == 'N') ? 1.0 : -1.0;
                            WEFactor = (TmpStr4.charAt(TmpStr4.length() - 1) == 'W') ? 1.0 : -1.0;
                            LatitudeArray[XInc] = NSFactor
                                    * Double.parseDouble(TmpStr3.substring(0, TmpStr3.length() - 1));
                            LongitudeArray[XInc] = WEFactor
                                    * Double.parseDouble(TmpStr4.substring(0, TmpStr4.length() - 1));
                            StrengthArray[XInc] = (double) Integer.parseInt(TmpStr5);
                            if (TimeArray[XInc] < TimeArray[XInc - 1]) {
                                MonthIntVal = MonthIntVal + 1;
                                /* YearIntVal=YearIntVal; */
                                if (MonthIntVal == 13) {
                                    YearIntVal = YearIntVal + 1;
                                    MonthIntVal = 1;
                                }
                                JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal,
                                        YearIntVal);
                                TimeArray[XInc] = Functions
                                        .calctime(JulianDayIntVal, HMSTimeIntVal);
                            }
                            forecastRec = in.nextLine();
                            tokens = forecastRec.split(delims6);
                            if (tokens[0].equals("REMARKS")) {
                                ICAOValidFcstTF = false;
                            }
                            XInc++;
                        }
                    }
                }
            } else if (ForecastFileType == 8) {
                /* BEST - best track b-deck file input */
                int LastDateValueInt = 1;
                while (in.hasNextLine()) {
                    String forecastRec = in.nextLine();
                    String[] tokens = forecastRec.split(delims);
                    /*
                     * AL, 04, 2010073106, , BEST, 0, 88N, 329W, 20, 1009, LO,
                     * 0, , 0, 0, 0, 0, 1011, 180, 75, 0, 0,
                     */
                    if (tokens[3].equals("BEST")) {
                        TmpStr1 = tokens[2];
                        TmpStr2 = tokens[5];
                        TmpStr3 = tokens[6];
                        TmpStr4 = tokens[7];
                        /*
                         * System.out.printf("1=%s* 2=%s* 3=%s* 4=%s*\n",TmpStr1,
                         * TmpStr2,TmpStr3,TmpStr4);
                         */
                        YearIntVal = Integer.parseInt(TmpStr1.substring(0, 4));
                        MonthIntVal = Integer.parseInt(TmpStr1.substring(4, 6));
                        DayIntVal = Integer.parseInt(TmpStr1.substring(6, 8));
                        HMSTimeIntVal = 10000 * Integer.parseInt(TmpStr1.substring(8, 10));
                        JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal, YearIntVal);
                        /*
                         * System.out.printf(
                         * "day=%d month=%d year=%d hms=%d julian=%d \n"
                         * ,DayIntVal
                         * ,MonthIntVal,YearIntVal,HMSTimeIntVal,JulianDayIntVal
                         * );
                         */
                        double TmpTimeVal1 = Functions.calctime(JulianDayIntVal, HMSTimeIntVal);
                        NSFactor = (TmpStr2.charAt(TmpStr2.length() - 1) == 'N') ? 1.0 : -1.0;
                        WEFactor = (TmpStr3.charAt(TmpStr3.length() - 1) == 'W') ? 1.0 : -1.0;
                        int CurrentDateValueInt = HMSTimeIntVal;
                        if (CurrentDateValueInt != LastDateValueInt) {
                            /*
                             * System.out.printf("tmptimeval=%f currenttime=%f\n"
                             * ,TmpTimeVal1,CurrentTime);
                             */
                            if (TmpTimeVal1 < CurrentTime) {
                                TimeArray[0] = TmpTimeVal1;
                                LatitudeArray[0] = NSFactor
                                        * Double.parseDouble(TmpStr2.substring(0,
                                        TmpStr2.length() - 1)) / 10.0;
                                LongitudeArray[0] = WEFactor
                                        * Double.parseDouble(TmpStr3.substring(0,
                                        TmpStr3.length() - 1)) / 10.0;
                                StrengthArray[0] = (double) Integer.parseInt(TmpStr4);
                            } else {
                                if ((TmpTimeVal1 >= CurrentTime) && (XInc < 5)) {
                                    XInc++;
                                    TimeArray[XInc] = TmpTimeVal1;
                                    LatitudeArray[XInc] = NSFactor
                                            * Double.parseDouble(TmpStr2.substring(0,
                                            TmpStr2.length() - 1)) / 10.0;
                                    LongitudeArray[XInc] = WEFactor
                                            * Double.parseDouble(TmpStr3.substring(0,
                                            TmpStr3.length() - 1)) / 10.0;
                                    StrengthArray[XInc] = (double) Integer.parseInt(TmpStr4);
                                }
                            }
                        }
                        LastDateValueInt = CurrentDateValueInt;
                    }
                }
            } else if (ForecastFileType == 9) {
                /* HURDAT - best track file input */
                while (in.hasNextLine()) {
                    String forecastRec = in.nextLine();
                    String[] tokens = forecastRec.split(delims);
                    TmpStr1 = tokens[2];
                    TmpStr2 = tokens[3];
                    TmpStr3 = tokens[4];
                    TmpStr4 = tokens[5];
                    TmpStr5 = tokens[6];
                    TmpStr6 = tokens[7];
                    TmpStr7 = tokens[8];
                    YearIntVal = Integer.parseInt(TmpStr1);
                    MonthIntVal = Integer.parseInt(TmpStr2);
                    DayIntVal = Integer.parseInt(TmpStr3);
                    HMSTimeIntVal = (10000 * Integer.parseInt(TmpStr4));
                    JulianDayIntVal = Functions.idmyyd(DayIntVal, MonthIntVal, YearIntVal);
                    double TmpTimeVal1 = Functions.calctime(JulianDayIntVal, HMSTimeIntVal);
                    /*
                     * NSFactor = (TmpStr5.charAt(TmpStr5.length()-1)=='N') ?
                     * 1.0 : -1.0;
                     */
                    /*
                     * WEFactor = (TmpStr6.charAt(TmpStr6.length()-1)=='W') ?
                     * 1.0 : -1.0;
                     */
                    NSFactor = 1.0;
                    WEFactor = 1.0;
                    if (TmpTimeVal1 < CurrentTime) {
                        TimeArray[0] = TmpTimeVal1;
                        LatitudeArray[0] = NSFactor
                                * Double.parseDouble(TmpStr5.substring(0, TmpStr5.length() - 1));
                        LongitudeArray[0] = WEFactor
                                * Double.parseDouble(TmpStr6.substring(0, TmpStr6.length() - 1));
                        StrengthArray[0] = (double) Integer.parseInt(TmpStr7);
                    } else {
                        if ((TmpTimeVal1 >= CurrentTime) && (XInc < 5)) {
                            XInc++;
                            TimeArray[XInc] = TmpTimeVal1;
                            LatitudeArray[XInc] = NSFactor
                                    * Double.parseDouble(TmpStr5.substring(0, TmpStr5.length() - 1));
                            LongitudeArray[XInc] = WEFactor
                                    * Double.parseDouble(TmpStr6.substring(0, TmpStr6.length() - 1));
                            StrengthArray[XInc] = (double) Integer.parseInt(TmpStr7);
                        }
                    }
                }
            } else {
                ReturnFlagValue = -2;
            }
        } catch (Exception e) {
            System.err.println("Failed to read Forecast file...");
            in.close();
            return null;
        }

        int ZInc;
        int MinFcstValue;
        int LastDateJulianHoldValue = 0;
        int CurrentYear = 0;
        double StrengthValue = 0;
        double LastDateValue = 0;
        boolean ChangeHemispheresTF = false;
        boolean YearChangeFlag = false;

        /* System.out.printf("returnflagvalue=%d\n",ReturnFlagValue); */
        if (ReturnFlagValue >= 0) {
            StrengthValue = StrengthArray[0];
            MinFcstValue = XInc;
            /* initialize forecast info array */
            for (ZInc = 0; ZInc < 5; ZInc++) {
                ForecastTimes[ZInc] = 9999999.5;
                ForecastLatitudes[ZInc] = 999.5;
                ForecastLongitudes[ZInc] = 999.5;
            }
            /* XInc--; */
            XInc = Math.min(5, XInc);
            /*
             * System.out.printf("%d  %f %f %f\n",0,TimeArray[0],LatitudeArray[0]
             * ,LongitudeArray[0]);
             */
            LastDateValue = TimeArray[0];
            ForecastTimes[0] = TimeArray[0];
            for (ZInc = 1; ZInc < XInc; ZInc++) {
                ForecastTimes[ZInc] = TimeArray[ZInc];
                if ((TimeArray[ZInc] - LastDateValue) > 100.0) {
                    /* there has been a year change during the forecast time */
                    if (!YearChangeFlag) {
                        LastDateJulianHoldValue = (int) LastDateValue;
                        YearChangeFlag = true;
                    }
                    CurrentYear = ((int) TimeArray[ZInc]) / 1000;
                    CurrentDayValue = TimeArray[ZInc] - (double) (CurrentYear * 1000);
                    TimeArray[ZInc] = (double) ((int) LastDateJulianHoldValue + CurrentDayValue);
                    LastDateValue = TimeArray[ZInc];
                }
                /*
                 * System.out.printf("%d  %f %f %f\n",ZInc,TimeArray[ZInc],
                 * LatitudeArray[ZInc],LongitudeArray[ZInc]);
                 */
                if (Math.abs(LongitudeArray[ZInc] - LongitudeArray[0]) > 180.0) {
                    /*
                     * System.out.printf(
                     * "changing hemispheres... adjusting longitude\n");
                     */
                    ChangeHemispheresTF = true;
                    if (LongitudeArray[0] < 0) {
                        LongitudeArray[ZInc] = -360.0 + LongitudeArray[ZInc];
                    } else {
                        LongitudeArray[ZInc] = 360.0 + LongitudeArray[ZInc];
                    }
                    /*
                     * System.out.printf("%d  %f %f %f\n",ZInc,TimeArray[ZInc],
                     * LatitudeArray[ZInc],LongitudeArray[ZInc]);
                     */
                }
            }
            if (YearChangeFlag) {
                /* System.out.printf("YearChangeFlag=TRUE\n"); */
                /* System.out.printf("CurrentTime=%f\n",CurrentTime); */
                /*
                 * need to adjust current image time to fit into adjusted date
                 * array
                 */
                if ((CurrentTime - TimeArray[0]) > 100.0) {
                    CurrentYear = ((int) CurrentTime) / 1000;
                    CurrentDayValue = CurrentTime - (double) (CurrentYear * 1000);
                    CurrentTime = (double) ((int) LastDateJulianHoldValue + CurrentDayValue);
                    /* System.out.printf("CurrentTime=%f\n",CurrentTime); */
                }
            }
            if (CurrentTime < TimeArray[0]) {
                /*
                 * check to see if latest image is only 15 minutes before
                 * current forecast time
                 */
                if (CurrentTime > (TimeArray[0] - .015))
                    TimeArray[0] = CurrentTime;
            }

            /* determine number of valid forecast times for given image time */
            YInc = 0;
            while ((TimeArray[YInc] <= CurrentTime) && (YInc < XInc)) {
                YInc++;
            }
            /*
             * System.out.printf(
             * "yinc=%d  xinc=%d  curtime=%f time0=%f threshtime=%f\n"
             * ,YInc,XInc,CurrentTime,TimeArray[0],ThresholdTime);
             */
            if ((YInc == 0) && (TimeArray[0] != CurrentTime)) {
                /*
                 * printf(
                 * "INVALID FORECAST FILE, IMAGE BEFORE/AFTER FORECAST TIMES\n"
                 * );
                 */
                System.out.printf("%d %f %f %d\n", -1, -99.99, -999.99, 99);
                ReturnFlagValue = -1;
            }
            if ((YInc == XInc) && (CurrentTime >= TimeArray[XInc])) {
                System.out.printf("%d %f %f %d\n", -2, -99.99, -999.99, 99);
                ReturnFlagValue = -1;
            }
            if (ReturnFlagValue >= 0) {
                if ((CurrentTime - ThresholdTime) > TimeArray[0]) {
                    /* printf("FORECAST FILE MORE THAN 24 HOURS OLD\n"); */
                    System.out.printf("%d %f %f %f\n", -3, -99.99, -999.99, -99.99);
                    ReturnFlagValue = -1;
                } else {
                    for (ZInc = 0; ZInc <= MinFcstValue; ZInc++) {
                        ForecastLatitudes[ZInc] = LatitudeArray[ZInc];
                        ForecastLongitudes[ZInc] = LongitudeArray[ZInc];
                    }

                    double InterpLatitude = adt_polint(TimeArray, LatitudeArray, XInc, CurrentTime);
                    double InterpLongitude = adt_polint(TimeArray, LongitudeArray, XInc,
                            CurrentTime);

                    if ((InterpLatitude <= -999.) || (InterpLongitude <= -999.)) {
                        System.out.printf("%d %f %f %f\n", -4, -99.99, -999.99, -99.99);
                        ReturnFlagValue = -1;
                    } else {
                        if (ChangeHemispheresTF) {
                            if (InterpLongitude > 180.0) {
                                InterpLongitude = -360.0 + InterpLongitude;
                            } else if (InterpLongitude < -180.0) {
                                InterpLongitude = 360.0 + InterpLongitude;
                            } else {
                                /* InterpLongitude=InterpLongitude; */
                            }
                        }
                        System.out.printf("%d %f %f %f\n", 1, InterpLatitude, InterpLongitude,
                                StrengthValue);
                        ReturnLatitudeInterpolation = InterpLatitude;
                        ReturnLongitudeInterpolation = InterpLongitude;
                        ReturnFlagValue = 0;
                    } /* ReturnFlagValue=-4 */
                } /* ReturnFlagValue=-3 */
            } /* ReturnFlagValue=-1/-2 */
        } /* ReturnFlagValue=-6 */

        in.close();

        ReturnCurrentIntensity = StrengthValue;

        /* McIDAS-V Conversion */
        ReturnLongitudeInterpolation = -1.0 * ReturnLongitudeInterpolation;
        logger.debug("ReadForecasts out...");

        return new double[] { (double) ReturnFlagValue, ReturnLatitudeInterpolation,
                ReturnLongitudeInterpolation, ReturnCurrentIntensity };

    }

    /**
     * Polynomial interpolation scheme program derived from FORTRAN program
     * POLINT in : Numerical Recipies - The Art of Scientific Computing, 1986,
     * Press, Flannery, Teukolsky, and Vetterling, Cambridge Press
     *
     * @param TimeArrayInput
     *            Array containing time values.
     * @param PositionArrayInput
     *            Array containing variable to interpolate.
     * @param NumberOfPointsInput
     *            Number of points in array.
     * @param CurrentTime
     *            Time to interpolate to.
     *
     * @return Interpolated position value. Note: if value is {@code -999.5}, it
     *         means that the denominator is 0, signifying that there was an
     *         interpolation error.
     */
    private static double adt_polint(double[] TimeArrayInput, double[] PositionArrayInput,
                                     int NumberOfPointsInput, double CurrentTime) {
        double PositionValue;
        double PositionError;
        double DenominatorValue;
        double TimeDifferenceValue1;
        double TimeDifferenceValue2;
        double TimeValue1;
        double TimeValue2;
        double[] LocalArray1 = new double[10];
        double[] LocalArray2 = new double[10];
        int XInc;
        int YInc;
        int StoreValue = 1;

        TimeDifferenceValue1 = Math.abs(CurrentTime - TimeArrayInput[0]);
        for (XInc = 1; XInc <= NumberOfPointsInput; XInc++) {
            TimeDifferenceValue2 = Math.abs(CurrentTime - TimeArrayInput[XInc - 1]);
            if (TimeDifferenceValue2 < TimeDifferenceValue1) {
                StoreValue = XInc;
                TimeDifferenceValue1 = TimeDifferenceValue2;
            }
            /*
             * System.out.printf("Xinc-1=%d position array=%f\n",XInc-1,
             * PositionArrayInput[XInc-1]);
             */
            LocalArray1[XInc - 1] = PositionArrayInput[XInc - 1];
            LocalArray2[XInc - 1] = PositionArrayInput[XInc - 1];
        }
        double PositionValueLocal = PositionArrayInput[StoreValue - 1];
        PositionError = 0.0;
        StoreValue--;
        for (YInc = 1; YInc <= NumberOfPointsInput - 1; YInc++) {
            for (XInc = 1; XInc <= NumberOfPointsInput - YInc; XInc++) {
                TimeValue1 = TimeArrayInput[XInc - 1] - CurrentTime;
                TimeValue2 = TimeArrayInput[(XInc - 1) + YInc] - CurrentTime;
                PositionValue = LocalArray1[XInc] - LocalArray2[XInc - 1];
                DenominatorValue = TimeValue1 - TimeValue2;
                if (DenominatorValue == 0.0) {
                    /* interpolation error - denominator = 0 */
                    return -999.5; /* EXIT */
                }
                DenominatorValue = PositionValue / DenominatorValue;
                LocalArray1[XInc - 1] = TimeValue1 * DenominatorValue;
                LocalArray2[XInc - 1] = TimeValue2 * DenominatorValue;
                /*
                 * System.out.printf(
                 * "YInc=%d XInc=%d  demon=%f local1=%f local2=%f\n"
                 * ,YInc,XInc,DenominatorValue
                 * ,LocalArray1[XInc-1],LocalArray2[XInc-1]);
                 */
            }
            if ((2 * StoreValue) < (NumberOfPointsInput - YInc)) {
                PositionError = LocalArray1[StoreValue];
            } else {
                PositionError = LocalArray2[StoreValue - 1];
                StoreValue--;
            }
            PositionValueLocal = PositionValueLocal + PositionError;
        }
        /*
         * System.out.printf("interpolated position=%f error=%f\n",
         * PositionValueLocal,PositionError);
         */
        double InterpolatedPosition = PositionValueLocal;

        /*
         * NOTE : additional function return point above - return -1 if
         * interpolation fails/denominator is zero
         */

        return InterpolatedPosition;

    }

}