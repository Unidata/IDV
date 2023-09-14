/*
 * $Id: CdmTrackAdapter.java,v 1.2 2007/04/16 20:34:57 jeffmc Exp $
 *
 * Copyright  1997-2023 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.sounding;



import ucar.nc2.constants.FeatureType;

import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.TrajectoryFeature;

import ucar.nc2.ft.point.standard.StandardTrajectoryCollectionImpl;
import ucar.nc2.ft.point.standard.TableAnalyzer;
import ucar.unidata.data.BadDataException;



import java.util.*;


/**
 * Adapter for track type data.  This could be a balloon sounding
 * with spatial (lat/lon/alt) data for each time step, or an
 * aircraft track.
 *
 * @author IDV Development Team
 * @version $Revision: 1.2 $ $Date: 2007/04/16 20:34:57 $
 */
public class CdmTrackAdapter extends TrackAdapter {


    /**
     * Construct a new track from the filename
     *
     *
     * @throws Exception    On badness
     */
     public CdmTrackAdapter( ) throws Exception {
    //        super(filename);
     }


    /**
    /**
     * Construct a new track from the filename
     *
     * @param filename  location of file
     * @param pointDataFilter Filters the variables to use
     * @param stride The stride
     * @param lastNMinutes use the last N minutes
     *
     * @throws Exception    On badness
     */
    public CdmTrackAdapter(TrackDataSource dataSource, String filename,
                           Hashtable pointDataFilter, int stride,
                           int lastNMinutes)
            throws Exception {
        super(dataSource, filename, pointDataFilter, stride, lastNMinutes);
        FeatureDatasetPoint tod = null;
        Formatter         buf = new Formatter();
        try {
            //tod = TrajectoryObsDatasetFactory.open(filename);
            tod = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.TRAJECTORY, filename, null, buf);
        } catch (java.io.FileNotFoundException fnfe) {
            throw new BadDataException("Could not open data source:"
                                       + filename);
        }

        if (tod.getFeatureType() == null) {
            throw new BadDataException("Could not open trajectory file:"
                                       + filename);
        }

        List trajectories = tod.getPointFeatureCollectionList();
        Iterator  itor = trajectories.iterator();
        while(itor.hasNext()) {
            StandardTrajectoryCollectionImpl trajectoryFeatureCollection = (StandardTrajectoryCollectionImpl)itor.next();
            for (TrajectoryFeature trajectoryFeature : trajectoryFeatureCollection) {
                addTrackInfo(
                        new CdmTrackInfo(
                                this, tod, trajectoryFeature));
            }
        }
    }




}

