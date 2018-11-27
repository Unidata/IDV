/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

package ucar.unidata.data;


/**
 * Created by yuanho on 9/11/15.
 */
public class GempakUtil {

    /** _more_ */
    public static final String[] wcod = {
        "A", "D", "F", "H", "K", "L", "N", "Q", "R", "S", "T", "V", "A-",
        "A+", "AP", "BD", "BN", "BS", "BY", "GF", "HD", "HF", "HK", "IC",
        "IF", "IN", "IP", "KD", "KF", "KH", "L-", "L+", "LR", "LS", "PO",
        "R-", "R+", "RL", "RS", "RT", "RW", "S-", "S+", "SG", "SP", "SW",
        "T+", "TA", "TD", "TR", "TS", "UP", "ZL", "ZR", "AP-", "AP+", "BD+",
        "BS+", "HGF", "IPW", "R-S", "RS-", "RW-", "RW+", "RWA", "SGW", "SP-",
        "SPW", "SW-", "SW+", "T-A", "T+A", "T-R", "T+R", "T-S", "T+S", "TBD",
        "TBN", "TR+", "TRW", "TSW", "ZL-", "ZL+", "ZR-", "ZR+", "IPW-",
        "IPW+", "L-R-", "L+R+", "L-S-", "L+S+", "R-IP", "R-L-", "R-S+",
        "R+S-", "R-S-", "R+S+", "RW-A", "RW+A", "RW-S", "RW+S", "RWSW",
        "SGW-", "SPW-", "T+BD", "T+BN", "T+RS", "TRW-", "TRW+", "TRWA",
        "TSW-", "TSW+", "ZLW-", "ZRW-", "FUNNE", "TORNA", "TRW-A", "TRW+A",
        "WATER"
    };

    /** _more_ */
    public static final int[] wysm = {
        90, 6, 10, 5, 4, 53, 7, 18, 63, 73, 17, 201, 89, 90, 88, 7, 7, 38,
        202, 12, 6, 10, 5, 78, 48, 76, 79, 6, 10, 5, 51, 55, 59, 53, 8, 61,
        65, 59, 69, 95, 81, 71, 75, 77, 88, 86, 17, 96, 98, 95, 105, 203, 57,
        67, 87, 88, 34, 39, 12, 88, 68, 68, 80, 81, 90, 77, 87, 88, 85, 86,
        96, 99, 95, 97, 105, 107, 98, 98, 97, 95, 105, 56, 57, 66, 67, 87, 88,
        58, 59, 68, 69, 79, 58, 69, 68, 68, 69, 89, 90, 83, 84, 84, 77, 87,
        98, 98, 107, 95, 97, 96, 105, 107, 56, 66, 19, 19, 96, 99, 19
    };

    /** _more_ */
    public static final String[] wpart = {
        "R", "L", "S", "A", "T", "H", "K", "D", "F", "Q", "V", " ", "R-",
        "R+", "ZR", "RW", "L-", "L+", "ZL", "S-", "S+", "SW", "IP", "SG",
        "SP", "A-", "A+", "T-", "T+", "IF", "GF", "BS", "BD", "BY", "BN",
        "IC", "IN", "AP", "KH", "PO", "UP", " ", " ", " ", " ", " ", " ", " ",
        "ZR-", "ZR+", "RW-", "RW+", "ZL-", "ZL+", "SW-", "SW+", "IP-", "IP+",
        "SG-", "SG+", "SP-", "SP+", "IPW", "IC-", "IC+", "TRW", "SPW", "BD+",
        "BN+", "BS+", " ", " ", " ", " ", "IPW-", "IPW+", "TRW-", "TRW+", " "
    };
    //DATA              i1strt, i1stop, i2strt, i2stop, i3strt, i3stop,
    //+                 i4strt, i4stop / 0, 10, 12, 40, 48, 66, 74, 77 /

    /**
     * _more_
     *
     * @param wthr _more_
     *
     * @return _more_
     */
    public static int pt_wsym(String wthr) {
        String buff = wthr.trim();
        int    wsym = 999;
        //      If there is no weather, return.
        //
        if (buff.equals(" ") || (buff.length() == 0)) {
            wsym = 999;
        }

        //      Check each code. Save longest match.
        int ilen;

        for (int i = 0; i <= 118; i++) {
            //
            // Find length of string to match.
            if (i <= 11) {
                ilen = 1;
            } else if (i <= 53) {
                ilen = 2;
            } else if (i <= 84) {
                ilen = 3;
            } else if (i <= 113) {
                ilen = 4;
            } else {
                ilen = 5;
            }
            if ((buff.length() >= ilen)
                    && buff.substring(0, ilen).equals(wcod[i])) {
                wsym = wysm[i];
            }

        }

        return wsym;
    }


    /**
     * _more_
     *
     * @param wnum _more_
     *
     * @return _more_
     */
    public static String pt_wcod(int wnum) {
        //      Check for special codes.
        //
        String pt_wcod;
        int    inum[] = new int[3];

        if (wnum == 0) {
            pt_wcod = " ";
            return pt_wcod;
        } else if (wnum == -1) {
            pt_wcod = "TORNA";
            return pt_wcod;
        } else if (wnum == -2) {
            pt_wcod = "FUNNE";
            return pt_wcod;
        } else if (wnum == -3) {
            pt_wcod = "WATER";
            return pt_wcod;
        }

        //      Break input into three numbers.
        //
        int num = wnum;
        inum[0] = Math.floorMod(num, 80);
        num     = (num - inum[0]) / 80;
        inum[1] = Math.floorMod(num, 80);
        num     = (num - inum[1]) / 80;
        inum[2] = num;
        //
        String part[] = new String[3];

        for (int i = 0; i < 3; i++) {
            //
            // Check that this is in the proper range.
            if ((inum[i] <= 0) || (inum[i] > 79)) {
                part[i] = " ";
            } else {
                part[i] = wpart[inum[i] - 1];
            }
        }
        //
        //      Combine strings and remove blanks.
        //
        String wthr = part[0].trim() + part[1].trim() + part[2].trim();

        pt_wcod = wthr.trim();

        return pt_wcod;
    }

    /**
     * _more_
     *
     * @param wthr _more_
     *
     * @return _more_
     */
    public static int wchar2wcode(String wthr) {

        int    fnum[] = { 0, 0, 0 };


        String input  = wthr.trim();
        int    length = input.length();
        int    iprt   = 0;
        while (length > 0) {
            //
            //      First check four character strings.
            //
            int num = 0;

            if (length >= 4) {
                String w4 = input.substring(0, 3);

                for (int i = 74; i <= 77; i++) {
                    if (w4.equals(wpart[i])) {
                        num = i + 1;
                    }
                }
            }
            //
            // If found, eliminate from string and fix length.

            if (num != 0) {
                input  = input.substring(4);
                length = length - 4;
                //
                // Otherwise, check three character symbols.
            } else {
                if (length >= 3) {
                    String w3 = input.substring(0, 3);
                    for (int i = 48; i <= 66; i++) {
                        if (w3.equals(wpart[i])) {
                            num = i + 1;
                        }
                    }
                }
                //
                // If found, eliminate from string and fix length.
                if (num != 0) {
                    input  = input.substring(3);
                    length = length - 3;
                    //
                    // Otherwise, check two character symbols.
                } else {
                    if (length >= 2) {
                        String w2 = input.substring(0, 2);
                        for (int i = 12; i <= 40; i++) {
                            if (w2.equals(wpart[i])) {
                                num = i + 1;
                            }
                        }
                    }
                    //
                    // If found, eliminate from string and fix length.
                    if (num != 0) {
                        input  = input.substring(2);
                        length = length - 2;
                        //
                        // Otherwise, check one character symbols.

                    } else {
                        if (length >= 1) {
                            String w1 = input.substring(0, 1);
                            for (int i = 0; i <= 10; i++) {
                                if (w1.equals(wpart[i])) {
                                    num = i + 1;
                                }
                            }
                        }
                        //
                        // If found, eliminate from string and fix length.

                        if (num != 0) {
                            input  = input.substring(1);
                            length = length - 1;
                            //
                            // Otherwise, an error was encountered.
                        } else {
                            length = 0;
                        }
                    }
                }
            }
            fnum[iprt] = num;
            iprt       = iprt + 1;

            if (iprt > 3) {
                length = 0;
            }

            if (input.startsWith("+") || input.startsWith("-")) {
                input  = input.substring(1);
                length = length - 1;
            }

        }

        //
        // Compute number.
        int PT_WNUM = fnum[2] * 80 * 80 + fnum[1] * 80 + fnum[0];

        return PT_WNUM;

    }

}
