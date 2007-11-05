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


/**
 * Class to hold the grid navigation information
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GridNavBlock {

    /** raw values */
    float[] vals = null;

    /**
     * Create a new grid nav block
     */
    public GridNavBlock() {}

    /**
     * Create a new grid nav block with the values
     *
     * @param words   analysis block values
     */
    public GridNavBlock(float[] words) {
        setValues(words);
    }

    /**
     * Set the grid nav block values
     *
     * @param values   the raw values
     */
    public void setValues(float[] values) {
        vals = values;
    }

    /**
     * Print out the navibation block so it looks something like this:
     * <pre>
     *        PROJECTION:          LCC
     *        ANGLES:                25.0   -95.0    25.0
     *        GRID SIZE:           93  65
     *        LL CORNER:              12.19   -133.46
     *        UR CORNER:              57.29    -49.38
     * </pre>
     *
     * @return  a String representation of this.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("\n    PROJECTION:         ");
        buf.append(GempakUtil.ST_ITOC(Float.floatToIntBits(vals[1])));
        buf.append("\n    ANGLES:             ");
        buf.append(vals[10]);
        buf.append("  ");
        buf.append(vals[11]);
        buf.append("  ");
        buf.append(vals[12]);
        buf.append("\n    GRID SIZE:          ");
        buf.append(vals[4]);
        buf.append("  ");
        buf.append(vals[5]);
        buf.append("\n    LL CORNER:          ");
        buf.append(vals[6]);
        buf.append("  ");
        buf.append(vals[7]);
        buf.append("\n    UR CORNER:          ");
        buf.append(vals[8]);
        buf.append("  ");
        buf.append(vals[9]);
        return buf.toString();
    }
}

