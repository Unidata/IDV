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


import visad.Text;

import visad.georef.EarthLocation;



import visad.georef.NamedLocation;


/**
 * Interface for point observations at a named location (Station observations)
 *
 * @author MetApps Development Team
 * @version $Revision: 1.9 $ $Date: 2006/12/01 20:42:34 $
 */
public interface StationOb extends PointOb {

    /**
     * Get the station associated with this observation.
     * @return ob's station
     */
    public NamedLocation getStation();

    /**
     * Get the identifier of the station (convenience method)
     * @return ob's station id
     */
    public Text getStationId();

}
