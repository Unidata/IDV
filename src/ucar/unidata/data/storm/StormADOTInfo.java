/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.storm;


import java.util.ArrayList;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Feb 25, 2009
 * Time: 2:23:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormADOTInfo {

    /* various constants */

    /** _more_          */
    static public int kstart_v72 = 24;  /* inner cloud region analysis radius (km) */

    /** _more_          */
    static public int kend_v72 = 136;  /* outer cloud region analysis radius (km) */

    /** _more_          */
    static public int kenda_v72 = 190;  /* automated cursor position analysis radius (km) */

    /** _more_          */
    static public int keyerM_v72 = 24;  /* outer eye region search radius (km) - Manual position */

    /** _more_          */
    static public int keyerA_v72 = 75;  /* outer eye region search radius (km) - Auto position */

    /** _more_          */
    static public int kres_v72 = 4;  /* width of the cloud region analysis rings */

    /** _more_          */
    static public int arfd_v72;  /* the FILE id for the image in question */

    /* global variables */
    /* char   eyetype_v72[7][20]={ "EYE","PINHOLE EYE","LARGE EYE","LARGE RAGGED","RAGGED","OBSCURED","NONE" }; */

    /** _more_          */
    static public String eyetype_v72[] = { "EYE", "PINHOLE EYE", "LARGE EYE",
                                           "NONE" };

    /** _more_          */
    static public String cloudtype_v72[] = {
        "UNIFORM CDO", "EMBEDDED CENTER", "IRREGULAR CDO", "CURVED BAND",
        "SHEAR", "EYE"
    };

    /** _more_          */
    static public String cbasin_v72[] = { "ATLANTIC", "PACIFIC " };

    /** _more_          */
    static public String cmon_v72[] = {
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT",
        "NOV", "DEC"
    };
    /* T#-Pressure/Wind relationships (Atlantic and Pacific) */
    /* increased from 73 to 83 to allow for >8.0 storms */

    /** _more_          */
    static public double tno_v72[] = {
        -9999., -8888., 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0,
        2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.0, 3.1, 3.2, 3.3, 3.4,
        3.5, 3.6, 3.7, 3.8, 3.9, 4.0, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8,
        4.9, 5.0, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 6.0, 6.1, 6.2,
        6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 7.0, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6,
        7.7, 7.8, 7.9, 8.0, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 9.0
    };

    /** _more_          */
    static public double pres_v72[][] = {
        /* Atlantic pressure relationship values */
        {
            -9999.0, -8888.0, 1014.0, 1013.6, 1013.2, 1012.8, 1012.4, 1012.0,
            1011.4, 1010.8, 1010.2, 1009.6, 1009.0, 1008.2, 1007.4, 1006.6,
            1005.8, 1005.0, 1004.0, 1003.0, 1002.0, 1001.0, 1000.0, 998.8,
            997.6, 996.4, 995.2, 994.0, 992.6, 991.2, 989.8, 988.4, 987.0,
            985.4, 983.8, 982.2, 980.6, 979.0, 977.2, 975.4, 973.6, 971.8,
            970.0, 968.0, 966.0, 964.0, 962.0, 960.0, 957.6, 955.2, 952.8,
            950.4, 948.0, 945.4, 942.8, 940.2, 937.6, 935.0, 932.2, 929.4,
            926.6, 923.8, 921.0, 918.0, 915.0, 912.0, 909.0, 906.0, 902.8,
            899.6, 896.4, 893.2, 890.0, 886.6, 883.2, 879.8, 876.4, 873.0,
            869.4, 865.8, 862.2, 858.6, 855.0
        },
        /* Pacific pressure relationship values */
        {
            -9999.0, -8888.0, 1005.0, 1004.6, 1004.2, 1003.8, 1003.4, 1003.0,
            1002.4, 1001.8, 1001.2, 1000.6, 1000.0, 999.4, 998.8, 998.2,
            997.6, 997.0, 995.8, 994.6, 993.4, 992.2, 991.0, 989.6, 988.2,
            986.8, 985.4, 984.0, 982.4, 980.8, 979.2, 977.6, 976.0, 974.0,
            972.0, 970.0, 968.0, 966.0, 963.6, 961.2, 958.8, 956.4, 954.0,
            951.4, 948.8, 946.2, 943.6, 941.0, 938.2, 935.4, 932.6, 929.8,
            927.0, 924.4, 921.8, 919.2, 916.6, 914.0, 910.8, 907.6, 904.4,
            901.2, 898.0, 894.2, 890.4, 886.6, 882.8, 879.0, 874.8, 870.6,
            866.4, 862.2, 858.0, 853.4, 848.8, 844.2, 839.6, 835.0, 830.0,
            825.0, 820.0, 815.0, 810.0
        }
    };

    /* Atlantic/Pacific pressure relationship values */

    /** _more_          */
    static public double wind_v72[] = {
        -9999.0, -8888.0, 25.0, 25.0, 25.0, 25.0, 25.0, 25.0, 26.0, 27.0,
        28.0, 29.0, 30.0, 31.0, 32.0, 33.0, 34.0, 35.0, 37.0, 39.0, 41.0,
        43.0, 45.0, 47.0, 49.0, 51.0, 53.0, 55.0, 57.0, 59.0, 61.0, 63.0,
        65.0, 67.4, 69.8, 72.2, 74.6, 77.0, 79.6, 82.2, 84.8, 87.4, 90.0,
        92.4, 94.8, 97.2, 99.6, 102.0, 104.6, 107.2, 109.8, 112.4, 115.0,
        117.4, 119.8, 122.2, 124.6, 127.0, 129.6, 132.2, 134.8, 137.4, 140.0,
        143.0, 146.0, 149.0, 152.0, 155.0, 158.0, 161.0, 164.0, 167.0, 170.0,
        173.0, 176.0, 179.0, 182.0, 185.0, 188.0, 191.0, 194.0, 197.0, 200.0
    };
    /* BD curve break points */

    /** _more_          */
    static public double ebd_v72[] = {
        30.0, 9.0, -30.0, -42.0, -54.0, -64.0, -70.0, -76.0, -80.0, -84.0,
        -100.0
    };

    /* AODT library global variables */

    /** _more_          */
    boolean odt_v72, olist_v72, oautomode_v72, override_v72;

    /** _more_          */
    boolean ostartstr_v72, oland_v72, osearch_v72;


    /** _more_          */
    float[][] spiralband_v72 = new float[2][37];

    /** _more_          */
    float fcstlat_v72, fcstlon_v72;

    /** _more_          */
    double fcsttime_v72, starttime_v72, endtime_v72;

    /** _more_          */
    String hfile_v72, fixfile_v72;

    /** _more_          */
    String diagnostics_v72;

    /** _more_          */
    IRData odthistoryfirst_v72;

    /** _more_          */
    IRData odtcurrent_v72;

    /** _more_          */
    public ArrayList<RingData> tcircfirst_v72;

    /** _more_          */
   // DataGrid areadata_v72;


    public StormADOTInfo() {}

    public static class ImgCoeffs {
       String  sat_id;
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
           chan= Integer.parseInt(toks.get(2));
           det= Integer.parseInt(toks.get(3));
           scal_m= Float.parseFloat(toks.get(4));
           scal_b= Float.parseFloat(toks.get(5));
           side= Float.parseFloat(toks.get(6));
           conv_n= Float.parseFloat(toks.get(7));
           conv_a= Float.parseFloat(toks.get(8));
           conv_b= Float.parseFloat(toks.get(9));
           conv_g= Float.parseFloat(toks.get(10));

       }

       public String getSat_id() {
           return sat_id;
       }


   }

    /**
     * Class RingData _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.1 $
     */
    public static  class RingData {

        /** _more_          */
        float dist;

        /** _more_          */
        float angle;

        /** _more_          */
        float temp;

        /**
         * _more_
         */
        RingData() {}

        /**
         * _more_
         *
         * @param dist _more_
         * @param angle _more_
         * @param temp _more_
         */
        RingData(float dist, float angle, float temp) {
            this.dist  = dist;
            this.angle = angle;
            this.temp  = temp;
        }


    }

    /**
     * Class DataGrid _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.1 $
     */


    public static class DataGrid {
        float[][] temp;

        /** _more_          */
        float[][] lat;

        /** _more_          */
        float[][] lon;

        /** _more_          */
        int numx;

        /** _more_          */
        int numy;

        public DataGrid(float[][] temp, float[][] lon, float[][] lat, int numx,
                 int numy) {
            this.temp = temp;
            this.lat  = lat;
            this.lon  = lon;
            this.numx = numx;
            this.numy = numy;
        }

        public float [][] getlons(){
            return lon;
        }
        public float [][] getlats(){
            return lat;
        }

    };

    /**
     * Class IRData _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.1 $
     */
     public static class IRData {

       int domain;

        /** _more_          */
        double date;

        /** _more_          */
       // int time;

        /** _more_          */
        float TrawO;

        /** _more_          */
        float Traw;

        /** _more_          */
        float Tfinal;

        /** _more_          */
        float Tfinal3;

        /** _more_          */
        float CI;

        /** _more_          */
        float eyet;

        /** _more_          */
        float warmt;

        /** _more_          */
        float cloudt;

        /** _more_          */
        float cloudt2;

        /** _more_          */
        float cwcloudt;

        /** _more_          */
        float latitude;

        /** _more_          */
        float longitude;

        /** _more_          */
        float warmlatitude;

        /** _more_          */
        float warmlongitude;

        /** _more_          */
        float eyecdosize;

        /** _more_          */
        float eyestdv;

        /** _more_          */
        float cloudsymave;

        /** _more_          */
        int sattype;

        /** _more_          */
        int eyescene;

        /** _more_          */
        int cloudscene;

        /** _more_          */
        int eyesceneold;

        /** _more_          */
        int cloudsceneold;

        /** _more_          */
        int rule9;

        /** _more_          */
        int rule8;

        /** _more_          */
        int land;

        /** _more_          */
        int eyefft;

        /** _more_          */
        int cloudfft;

        /** _more_          */
        int cwring;

        /** _more_          */
        int ringcb;

        /** _more_          */
        int ringcbval;

        /** _more_          */
        int ringcbvalmax;

        /** _more_          */
        float ringcblatmax;

        /** _more_          */
        float ringcblonmax;

        /** _more_          */
        float CIadjp;
        /*float sst;*/
        /*float TIEraw;*/
        /*float TIEavg;*/
        /*int   TIEflag;*/

        /** _more_          */
        int autopos;

        /** _more_          */
        int LBflag;

        /** _more_          */
        int rapiddiss;

        /** _more_          */
        float rmw;

        /** _more_          */
        char[] comment = new char[50];

        /**
         * _more_
         */
        IRData() {}

        /**
         * _more_
         *
         * @param date _more_
         * @param TrawO _more_
         * @param Traw _more_
         * @param Tfinal _more_
         * @param Tfinal3 _more_
         * @param CI _more_
         * @param eyet _more_
         * @param warmt _more_
         * @param cloudt _more_
         * @param cloudt2 _more_
         * @param cwcloudt _more_
         * @param latitude _more_
         * @param longitude _more_
         * @param warmlatitude _more_
         * @param warmlongitude _more_
         * @param eyecdosize _more_
         * @param eyestdv _more_
         * @param cloudsymave _more_
         * @param sattype _more_
         * @param eyescene _more_
         * @param cloudscene _more_
         * @param eyesceneold _more_
         * @param cloudsceneold _more_
         * @param rule9 _more_
         * @param rule8 _more_
         * @param land _more_
         * @param eyefft _more_
         * @param cloudfft _more_
         * @param cwring _more_
         * @param ringcb _more_
         * @param ringcbval _more_
         * @param ringcbvalmax _more_
         * @param ringcblatmax _more_
         * @param ringcblonmax _more_
         * @param CIadjp _more_
         * @param autopos _more_
         * @param LBflag _more_
         * @param rapiddiss _more_
         * @param rmw _more_
         * @param comment _more_
         */
        IRData(double date, float TrawO, float Traw, float Tfinal,
                float Tfinal3, float CI, float eyet, float warmt,
                float cloudt, float cloudt2, float cwcloudt, float latitude,
                float longitude, float warmlatitude, float warmlongitude,
                float eyecdosize, float eyestdv, float cloudsymave,
                int sattype, int eyescene, int cloudscene, int eyesceneold,
                int cloudsceneold, int rule9, int rule8, int land,
                int eyefft, int cloudfft, int cwring, int ringcb,
                int ringcbval, int ringcbvalmax, float ringcblatmax,
                float ringcblonmax, float CIadjp, int autopos, int LBflag,
                int rapiddiss, float rmw, char[] comment) {

            this.date          = date;

            this.TrawO         = TrawO;
            this.Traw          = Traw;
            this.Tfinal        = Tfinal;
            this.Tfinal3       = Tfinal3;
            this.CI            = CI;
            this.eyet          = eyet;
            this.warmt         = warmt;
            this.cloudt        = cloudt;
            this.cloudt2       = cloudt2;
            this.cwcloudt      = cwcloudt;
            this.latitude      = latitude;
            this.longitude     = longitude;
            this.warmlatitude  = warmlatitude;
            this.warmlongitude = warmlongitude;
            this.eyecdosize    = eyecdosize;
            this.eyestdv       = eyestdv;
            this.cloudsymave   = cloudsymave;
            this.sattype       = sattype;
            this.eyescene      = eyescene;
            this.cloudscene    = cloudscene;
            this.eyesceneold   = eyesceneold;
            this.cloudsceneold = cloudsceneold;
            this.rule9         = rule9;
            this.rule8         = rule8;
            this.land          = land;
            this.eyefft        = eyefft;
            this.cloudfft      = cloudfft;
            this.cwring        = cwring;
            this.ringcb        = ringcb;
            this.ringcbval     = ringcbval;
            this.ringcbvalmax  = ringcbvalmax;
            this.ringcblatmax  = ringcblatmax;
            this.ringcblonmax  = ringcblonmax;
            this.CIadjp        = CIadjp;
            /*float sst;*/
            /*float TIEraw;*/
            /*float TIEavg;*/
            /*int   TIEflag;*/
            this.autopos   = autopos;
            this.LBflag    = LBflag;
            this.rapiddiss = rapiddiss;
            this.rmw       = rmw;
            this.comment   = comment;
        }
    }

}

