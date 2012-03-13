/*
 *
 * Copyright  1997-2012 Unidata Program Center/University Corporation for
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

//~--- non-JDK imports --------------------------------------------------------

import visad.CommonUnit;
import visad.Unit;

/**
 * A class to hold vertical scale settings for a VisAD display from the
 * dialog box VertScaleDialog class. They are public member
 * data to simply access.
 *
 * @author   IDV Development Team
 * @version  $Revision: 1.6 $
 */
public class VertScaleInfo {

    /** maximum range of the vertical scale */
    public double maxVertScale;

    /** minimum range of the vertical scale */
    public double minVertScale;

    /** Units of the range values */
    public Unit unit;

    /** Is visible */
    public boolean visible;

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

    @Override

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int prime  = 31;
        int       result = 1;
        long      temp;

        temp   = Double.doubleToLongBits(maxVertScale);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp   = Double.doubleToLongBits(minVertScale);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((unit == null)
                                   ? 0
                                   : unit.hashCode());
        result = prime * result + (visible
                                   ? 1231
                                   : 1237);

        return result;
    }

    @Override

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        VertScaleInfo other = (VertScaleInfo) obj;

        if (Double.doubleToLongBits(maxVertScale) != Double.doubleToLongBits(other.maxVertScale)) {
            return false;
        }

        if (Double.doubleToLongBits(minVertScale) != Double.doubleToLongBits(other.minVertScale)) {
            return false;
        }

        if (unit == null) {
            if (other.unit != null) {
                return false;
            }
        } else if (!unit.equals(other.unit)) {
            return false;
        }

        if (visible != other.visible) {
            return false;
        }

        return true;
    }
}
