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

package ucar.unidata.view.geoloc;


/**
 * AxisScaleInfo JavaBean.  Only here so old bundles don't break
 * @deprecated
 * @see LatLonAxisScaleInfo
 */
public class AxisScaleInfo extends LatLonAxisScaleInfo {

    /**
     * Instantiates a new lat lon scale info.
     */
    public AxisScaleInfo() {
        super();
    }

    /**
     * Set the format
     *
     * @param coordFormat  the format
     */
    @Deprecated  //9-5-2012
    public void setCoordFormat(CoordSys coordFormat) {
        super.setCoordFormat(COORD_FORMATS[0]);
    }

    @Deprecated  //9-5-2012
    public enum CoordSys {
        A, B, C, D, E, F, G, H;
    }

}
