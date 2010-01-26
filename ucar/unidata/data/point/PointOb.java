/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.point;



import visad.Data;
import visad.DateTime;

import visad.georef.EarthLocation;


/**
 * Abstraction for a point observation (values at a lat, lon, (alt) & time)
 * would handle the most generic thing like a lightning flash, or something
 * more specific like a METAR.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.12 $ $Date: 2006/12/01 20:42:34 $
 */
public interface PointOb extends Data {

    /** Used when finding indices of param names in the tuple */
    public static final int BAD_INDEX = -1;

    /** Parameter name for  latitude */
    public static final String PARAM_LAT = "lat";

    /** Parameter name for  longitude */
    public static final String PARAM_LON = "lon";

    /** Parameter name for  altitude */
    public static final String PARAM_ALT = "alt";

    /** Parameter name for  time */
    public static final String PARAM_TIME = "time";

    /** Parameter name for  the id */
    public static final String PARAM_ID = "ID";

    /** Parameter name for the id */
    public static final String PARAM_IDN = "IDN";


    /**
     * Get the location (lat/lon/alt) of the observation.
     * @return georeference location
     */
    public EarthLocation getEarthLocation();

    /**
     * Get the time of the observation.
     * @return  time the observation was taken
     */
    public DateTime getDateTime();

    /**
     * Get the data associated with this observation.
     * @return observed data for this location and time.
     */
    public Data getData();

}
