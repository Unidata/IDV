/*
 * $Id: VertScaleInfo.java,v 1.6 2006/03/22 16:16:41 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.view.geoloc;



import visad.Unit;
import visad.CommonUnit;

import ucar.unidata.util.Misc;


/**
 * A class to hold vertical scale settings for a VisAD display from the
 * dialog box VertScaleDialog class. They are public member
 * data to simply access.
 *
 * @author   IDV Development Team
 * @version  $Revision: 1.6 $
 */
public class VertScaleInfo {

    /** minimum range of the vertical scale */
    public double minVertScale;

    /** maximum range of the vertical scale */
    public double maxVertScale;

    /** Units of the range values */
    public Unit unit;

    /**
     * Construct a <code>VertScaleInfo</code> with the specified range.
     * Unit is assumed to be meters.
     *
     * @param min  minimum of the range
     * @param max  maximum of the range
     */
    public VertScaleInfo(double min, double max) {
        this(min, max, CommonUnit.meter);
    }

    /**
     * Construct a <code>VertScaleInfo</code> with the specified range..
     *
     * @param min  minimum of the range
     * @param max  maximum of the range
     * @param unit unit of range values
     */
    public VertScaleInfo(double min, double max, Unit unit) {
        this.minVertScale = min;
        this.maxVertScale = max;
        this.unit         = unit;
    }

    /**
     * is equals
     *
     * @param obj object
     *
     * @return is equals
     */
    public boolean equals(Object obj) {
        if ( !(obj instanceof VertScaleInfo)) {
            return false;
        }
        VertScaleInfo that = (VertScaleInfo) obj;
        return (this.minVertScale == that.minVertScale)
               && (this.maxVertScale == that.maxVertScale)
               && Misc.equals(this.unit, that.unit);

    }

}
