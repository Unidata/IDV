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

package ucar.unidata.data.radar;


import ucar.unidata.data.*;

import visad.*;

import java.rmi.RemoteException;

import java.util.Hashtable;


/**
 * A data adapter for radar data.
 * @author IDV Development Team @ ATD
 * @version $Revision: 1.10 $
 */
public interface RadarAdapter extends RadarConstants {

    /** RealType for Azimuth */
    public static RealType AZIMUTH_TYPE = RealType.getRealType(AZIMUTH,
                                              CommonUnit.degree);

    /** RealType for Range */
    public static RealType RANGE_TYPE = RealType.getRealType(RANGE,
                                            CommonUnit.meter);

    /** RealType for Elevation angle */
    public static RealType ELEVATION_ANGLE_TYPE =
        RealType.getRealType(ELEVATION_ANGLE, CommonUnit.degree);

    /** RealType for Azimuth angle */
    public static RealType AZIMUTH_ANGLE_TYPE =
        RealType.getRealType(AZIMUTH_ANGLE, CommonUnit.degree);

    /**
     * Get the data for the given DataChoice and selection criteria.
     * @param dataChoice         DataChoice for selection
     * @param subset             subsetting criteria
     * @param requestProperties  extra request properties
     * @return  the Data object for the request
     *
     * @throws RemoteException couldn't create a remote data object
     * @throws VisADException  couldn't create the data
     */
    public DataImpl getData(DataChoice dataChoice, DataSelection subset,
                            Hashtable requestProperties)
     throws VisADException, RemoteException;

    /**
     * Get the base time for this sweep
     *
     * @return time of sweep
     */
    public abstract DateTime getBaseTime();

    /**
     * Return the name of this adapter
     *
     * @return name
     */

    public String getName();

    /**
     * This is the hook to clear any cached data this adapter may be holding.
     * It gets called by the RadarDataSource
     */
    public void clearCachedData();

    /**
     * Clean up whatever we need to when we are removed.
     */
    public void doRemove();
}
