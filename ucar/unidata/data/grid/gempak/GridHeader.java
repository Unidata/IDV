/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 * 
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.grid.gempak;


import ucar.unidata.util.StringUtil;


/**
 * A class to hold grid header information
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GridHeader {

    /** Time 1 */
    public String time1;

    /** Time 2 */
    public String time2;

    /** Level 1 */
    public int level1 = GempakConstants.IMISSD;

    /** Level 2 */
    public int level2 = GempakConstants.IMISSD;

    /** coordinate type */
    public int ivcord;

    /** parameter */
    public String param;

    /** grid number */
    public int gridNumber;  // column

    /** packing type */
    public int packingType;

    /**
     * Create a grid header from the integer bits
     * @param number  grid number
     * @param header integer bits
     */
    public GridHeader(int number, int[] header) {
        gridNumber = number;
        int[] times1 = GempakUtil.TG_FTOI(header, 0);
        time1 = GempakUtil.TG_ITOC(times1);
        int[] times2 = GempakUtil.TG_FTOI(header, 2);
        time2  = GempakUtil.TG_ITOC(times2);
        level1 = header[4];
        level2 = header[5];
        ivcord = header[6];
        param = GempakUtil.ST_ITOC(new int[] { header[7], header[8],
                header[9] });

    }

    /**
     * Get a String representation of this object
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(StringUtil.padLeft(String.valueOf(gridNumber), 5));
        buf.append(StringUtil.padLeft(time1, 20));
        buf.append(" ");
        buf.append(StringUtil.padLeft(time2, 20));
        buf.append(" ");
        buf.append(StringUtil.padLeft(String.valueOf(level1), 5));
        if (level2 != -1) {
            buf.append(StringUtil.padLeft(String.valueOf(level2), 5));
        } else {
            buf.append("     ");
        }
        buf.append("  ");
        buf.append(StringUtil.padLeft(GempakUtil.LV_CCRD(ivcord), 6));
        buf.append(" ");
        buf.append(param.trim());
        buf.append(" ");
        buf.append(GempakUtil.getGridPackingName(packingType));
        return buf.toString();
    }

}
