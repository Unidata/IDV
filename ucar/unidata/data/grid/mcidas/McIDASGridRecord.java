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

package ucar.unidata.data.grid.mcidas;


import edu.wisc.ssec.mcidas.GridDirectory;

import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.unidata.data.grid.gempak.GridRecord;

import java.util.Date;


/**
 * A class to hold McIDAS grid record information
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class McIDASGridRecord implements GridRecord {

    /** grid information */
    private GridDirectory directory;

    /** offset to header */
    private int offsetToHeader;  // offset to header

    /** reference time as a Date */
    private Date refTime;

    /** valid time offset in ninutes */
    private int validOffset;

    /** actual valid time */
    private Date validTime;


    /**
     * Create a grid header from the integer bits
     * @param offset  offset to grid header in file
     * @param gd    grid directory
     */
    public McIDASGridRecord(int offset, GridDirectory gd) {
        offsetToHeader = offset;
        directory  = gd;
        /*
        int ymd = times1[0];
        if (ymd / 10000 < 50) {
            ymd += 20000000;
        } else if (ymd / 10000 < 100) {
            ymd += 19000000;
        }
        int hms = times1[1] * 100;  // need to add seconds
        refTime = new Date(McIDASUtil.mcDateHmsToSecs(ymd, hms) * 1000l);
        int offset = times1[2] % 100000;
        if ((offset == 0) || (offset % 100 == 0)) {  // 0 or no minutes
            validOffset = (offset / 100) * 60;
        } else {                                     // have minutes
            validOffset = (offset / 100) * 60 + offset % 100;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        calendar.setTime(refTime);
        calendar.add(Calendar.MINUTE, validOffset);
        validTime = calendar.getTime();
        */
    }

    /**
     * Get the first level of this GridRecord
     *
     * @return the first level value
     */
    public double getLevel1() {
        return directory.getLevelValue();
    }

    /**
     * Get the second level of this GridRecord
     *
     * @return the second level value
     */
    public double getLevel2() {
        return directory.getSecondLevelValue();
    }

    /**
     * Get the type for the first level of this GridRecord
     *
     * @return level type
     */
    public int getLevelType1() {
        return 0;  // TODO
    }

    /**
     * Get the type for the second level of this GridRecord
     *
     * @return level type
     */
    public int getLevelType2() {
        return 0;  // TODO
    }

    /**
     * Get the first reference time of this GridRecord
     *
     * @return reference time
     */
    public Date getReferenceTime() {
        return directory.getReferenceTime();
    }

    /**
     * Get the valid time for this grid.
     *
     * @return valid time
     */
    public Date getValidTime() {
        return directory.getValidTime();
    }

    /**
     * Get valid time offset (minutes) of this GridRecord
     *
     * @return time offset
     */
    public int getValidTimeOffset() {
        return directory.getForecastHour();
    }

    /**
     * Get the parameter name
     *
     * @return parameter name
     */
    public String getParameterName() {
        return directory.getParamName();
    }

    /**
     * Get the grid def record id
     *
     * @return parameter name
     */
    public String getGridDefRecordId() {
        return null;  // TODO
    }

    /**
     * Get the offset to the grid header (4 byte words)
     *
     * @return word offset
     */
    public int getOffsetToHeader() {
        return offsetToHeader;
    }

    /**
     * Get a String representation of this object
     * @return a String representation of this object
     */
    public String toString() {
        return directory.toString();
        /*
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
        buf.append(param);
        buf.append(" ");
        buf.append(GempakUtil.getGridPackingName(packingType));
        return buf.toString();
        */
    }

}

