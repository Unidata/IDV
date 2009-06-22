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

package ucar.unidata.idv.control.storm;


import java.util.ArrayList;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Mar 9, 2009
 * Time: 2:16:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormADOTUtil {


    /**
     * _more_
     *
     * @param odtcurrent _more_
     * @param domain _more_
     *
     * @return _more_
     */
    public static List aodtv72_textscreenoutput(
            StormADOTInfo.IRData odtcurrent, int domain)
    /* Output information to text screen.
        Inputs  : global structure odtcurrent_v72 containting current image information
        Outputs : none
    */
    {

        int      day, mon, year, ibasin, iok;
        int      degree, minute, second,
                 xr8  = 0,
                 xr9  = 0,
                 xrpd = 0;
        int      cloudcat, eyecat, cfft;
        int      ptr, ptr2, nptr;
        String   cdate, ctime, clat, clon;
        String   cmin, csec, clatmax, clonmax;
        char     iout2;
        String   latc = "N",
                 lonc = "W";
        String[] cr9  = { "OFF   ", "ON    ", "WEAKEN" };
        String[] crpd = { "OFF   ", "FLAG  ", "ON    ", "ON    " };
        String[] cbd  = {
            "LOW CLD", "OFF WHT", "DK GRAY", "MD GRAY", "LT GRAY", "BLACK  ",
            "WHITE  "
        };
        String[] cr8 = {
            "NO LIMIT ", "0.5T/6hr ", "1.2T/6hr ", "1.7T/12hr", "2.2T/18hr",
            "2.7T/24hr", "         ", "         ", "0.1T/hour", "0.5T/hour",
            "NO LIMIT ", "0.5T/6hr ", "1.7T/6hr ", "2.2T/12hr", "2.7T/18hr",
            "3.2T/24hr", "         ", "         ", "0.1T/hour", "0.5T/hour",
            "NO LIMIT ", "0.5T/6hr ", "0.7T/6hr ", "1.2T/12hr", "1.7T/18hr",
            "2.2T/24hr", "         ", "         ", "0.1T/hour", "0.5T/hour"
        };
        String[] basin = { "ATLANTIC    ", "WEST PACIFIC", "EAST PACIFIC",
                           "INDIAN      " };
        String[] cuse = {
            "MANUAL", "FORECAST INTERPOLATION", "LAPLACIAN ANALYSIS",
            "WARMEST PIXEL SEARCH", "SPIRAL ANALYSIS",
            "RING/SPIRAL COMBINATION", "LINEAR EXTRAPOLATION"
        };
        String  scenetype;
        String  scenetypemax;
        String  scenetypemaxll;
        String  eyermw;
        String  version;
        float   pwip, pwiw, cloudtemp, arcd, arcdmax, sdist, xlat, xlon, m;
        boolean bettercb = false,
                rmwflag  = false;



        /* convert Julian date/time to day/month/year format */
        int[] out = aodtv72_yddmy(odtcurrent.date);
        day  = out[0];
        mon  = out[1];
        year = out[2];
        /* format character string for date output */
        cdate = Integer.toString(day) + "-" + Integer.toString(mon) + "-"
                + Integer.toString(year);

        /* format character string for time output */

        ctime = Integer.toString(odtcurrent.time);


        /* convert xx.xxxx latitude format to degree/minute/second format */
        xlat   = odtcurrent.latitude;
        xlon   = odtcurrent.longitude;
        out    = aodtv72_lldms(xlat);
        degree = out[0];
        minute = out[1];
        second = out[2];

        if (xlat < 0.0) {
            latc = "S";
        }
        /* format character string for latitude output */

        clat = Integer.toString(degree) + "-" + Integer.toString(minute)
               + "-" + Integer.toString(second) + latc;

        /* convert xx.xxxx longitude format to degree/minute/second format */
        out    = aodtv72_lldms(xlon);
        degree = out[0];
        minute = out[1];
        second = out[2];
        if (xlon < 0.0) {
            lonc = "E";
        }
        /* format character string for longitude output */

        clon = Integer.toString(degree) + "-" + Integer.toString(minute)
               + "-" + Integer.toString(second) + lonc;

        /* determine current ocean basin in which storm is located */
        //ibasin=aodtv72_oceanbasin(xlat,xlon);

        /* determine Dvorak pressure/wind speed in relation to final CI # */
        pwip = aodtv72_getpwval(0, odtcurrent.CI, domain);
        pwiw = aodtv72_getpwval(1, odtcurrent.CI, domain);

        /* determine Rule 8 and Rule 9 screen output values */
        xr8 = odtcurrent.rule8;
        if (odtcurrent.rule9 == 1) {
            xr9 = 1;
        }
        xrpd      = odtcurrent.rapiddiss;
        cloudtemp = odtcurrent.cloudt;
        /* determine scenetype to be output to screen */
        eyecat   = odtcurrent.eyescene;
        cloudcat = odtcurrent.cloudscene;
        cfft     = odtcurrent.cloudfft;
        if (cloudcat == 2) {
            scenetype = StormADOTInfo.cloudtype_v72[cloudcat];
        } else if (cloudcat == 3) {
            arcd    = (float) (odtcurrent.ringcbval - 1) / 24.0f;
            arcdmax = (float) (odtcurrent.ringcbvalmax - 1) / 25.0f;
            if (arcdmax > arcd) {
                bettercb = true;
            }
            scenetype = "CURVED BAND with " + arcd + " ARC in "
                        + cbd[odtcurrent.ringcb];

            if (bettercb) {
                scenetypemax = "Maximum CURVED BAND with " + arcdmax
                               + " ARC in " + cbd[odtcurrent.ringcb];

                /* convert xx.xxxx latitude format to degree/minute/second format */
                out    = aodtv72_lldms(odtcurrent.ringcblatmax);
                degree = out[0];
                minute = out[1];
                second = out[2];

                if (odtcurrent.ringcblatmax < 0.0) {
                    latc = "S";
                }
                /* format character string for latitude output */
                clatmax = Integer.toString(degree) + "-"
                          + Integer.toString(minute) + "-"
                          + Integer.toString(second) + latc;


                /* convert xx.xxxx longitude format to degree/minute/second format */
                out    = aodtv72_lldms(odtcurrent.ringcblonmax);
                degree = out[0];
                minute = out[1];
                second = out[2];

                if (odtcurrent.ringcblonmax < 0.0) {
                    lonc = "E";
                }
                /* format character string for longitude output */
                clonmax = Integer.toString(degree) + "-"
                          + Integer.toString(minute) + "-"
                          + Integer.toString(second) + lonc;

                scenetypemaxll = " at Lat:" + clatmax + " Lon:" + clonmax;

            }
        } else if (cloudcat == 4) {
            sdist = odtcurrent.eyecdosize / 110.0f;
            if (sdist < 1.30) {
                scenetype = "SHEAR (%4.2f^ TO DG)* " + sdist;
            } else {
                scenetype = "SHEAR (>1.25^ TO DG)*";
            }

        } else {
            if (eyecat <= 2) {
                scenetype = StormADOTInfo.eyetype_v72[eyecat];
                if (eyecat <= 2) {
                    rmwflag = true;
                }

                if (odtcurrent.rmw < 0.0) {
                    if (eyecat == 1) {
                        eyermw = "<10";
                    } else {
                        eyermw = "N/A";
                    }
                } else {
                    eyermw = Integer.toString((int) odtcurrent.rmw);
                    /* if(eyecat==1) sprintf(eyermw,"<10"); */
                }
            } else {
                scenetype = "CLOUD REGION* "
                            + StormADOTInfo.cloudtype_v72[cloudcat];
            }

        }


        List result = new ArrayList();

        /* send results to the screen */
        result.add(
            "<tr><td>"
            + "\n****************************************************\n"
            + "</td></tr>");
        result.add("<tr><td>" + " ADVANCED DVORAK TECHNIQUE       \n"
                   + "</td></tr>");
        result.add("<tr><td>"
                   + " Tropical Cyclone Intensity Algorithm       \n\n"
                   + "</td></tr>");
        result.add("<tr><td>" + " ----- Current Analysis ----- \n"
                   + "</td></tr>");
        result.add("<tr><td>" + " Date : " + cdate + "Time : " + ctime + "\n"
                   + "</td></tr>");
        result.add("<tr><td>" + " Lat : " + clat + "Lon : " + clon + "\n"
                   + "</td></tr>");
        if ((odtcurrent.land == 1)) {
            result.add("<tr><td>"
                       + "               TROPICAL CYCLONE OVER LAND\n"
                       + "</td></tr>");
            result.add("<tr><td>"
                       + "               NO ADT ANALYSIS AVAILABLE\n"
                       + "</td></tr>");
        } else {
            result.add("<tr><td>" + " CI# /Pressure/ Vmax\n" + "</td></tr>");
            result.add("<tr><td>" + odtcurrent.CI + "  "
                       + (pwip + odtcurrent.CIadjp) + " " + pwiw + "\n"
                       + "</td></tr>");


            result.add("<tr><td>" + "Latitude bias adjustment to MSLP : "
                       + odtcurrent.CIadjp + "\n" + "</td></tr>");
            result.add("<tr><td>" + " Center Temp : " + odtcurrent.eyet
                       + "Cloud Region Temp : " + cloudtemp + "\n"
                       + "</td></tr>");
            result.add("<tr><td>" + " Scene Type : " + scenetype + "\n"
                       + "</td></tr>");


        }


        result.add(
            "<tr><td>"
            + "\n****************************************************\n"
            + "</td></tr>");

        return result;

    }

    /**
     * _more_
     *
     * @param llval _more_
     *
     * @return _more_
     */
    public static int[] aodtv72_lldms(float llval)
    /* Convert degree.degree to degree/minute/second format.
       Inputs  : llval  - latitude/longitude to convert
       Outputs : degree - degrees
                 minute - minutes
                 second - seconds
    */
    {
        int   deg;
        float min, sec;

        deg = (int) llval;
        min = (llval - (float) deg) * 60.0f;
        sec = (min - (float) ((int) min)) * 60.0f;

        int[] out = { deg, (int) min, (int) sec };

        return out;
    }

    /**
     * _more_
     *
     * @param syd _more_
     *
     * @return _more_
     */
    public static int[] aodtv72_yddmy(int syd)
    /* Convert yyyyddd to dd/mm/yy format.
       Inputs  : syd   - Julian day (yyyyddd)
       Outputs : day   - date
                 month - month
                 year  - year (yyyy)
    */
    {
        int[][] dn  = {
            {
                0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365
            }, {
                0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366
            }
        };
        int     iyy, idd, imm,
                ily = 0;

        iyy = syd / 1000;
        if (iyy < 1900) {
            if (iyy > 70) {
                iyy = iyy + 1900;
            } else {
                iyy = iyy + 2000;
            }
        }
        idd = (syd % 1000);
        if ((iyy % 4) == 0) {
            ily = 1;
        }
        for (imm = 0; imm < 13; imm++) {
            if (idd <= dn[ily][imm]) {
                break;
            }
        }

        int[] out = { idd - dn[ily][imm - 1], imm, iyy };
        return out;

    }

    /**
     * _more_
     *
     * @param ival _more_
     * @param cival _more_
     * @param idomain_v72 _more_
     *
     * @return _more_
     */
    public static float aodtv72_getpwval(int ival, float cival,
                                         int idomain_v72)
    /* Obtain pressure or wind_v72 value (for Atlantic or
       West Pacific storms) given the intensity estimate
       value.
        Inputs  : ival  - flag for wind_v72 (1) or pressure (0) output
                  cival - Current Intensity (CI) value
        Outputs : return value is pressure/wind_v72 value
    */
    {
        float value;
        int   ixx = 2;

        /* determine correct pressure/wind_v72 array bin */
        while ((cival > StormADOTInfo.tno_v72[ixx]) && (ixx < 82)) {
            ixx++;
        }

        /* convert CI value to wind_v72/pressure value */
        if (ival == 1) {
            value = (float) StormADOTInfo.wind_v72[ixx];  /* WIND */
        } else {
            value = (float) StormADOTInfo.pres_v72[idomain_v72][ixx];  /* PRESSURE */
        }

        return value;
    }

}


