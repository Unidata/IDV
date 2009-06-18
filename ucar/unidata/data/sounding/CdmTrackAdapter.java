/*
 * $Id: CdmTrackAdapter.java,v 1.2 2007/04/16 20:34:57 jeffmc Exp $
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

package ucar.unidata.data.sounding;



import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.Index0D;
import ucar.ma2.Range;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TrajectoryObsDatatype;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.TypedDatasetFactory;

import ucar.nc2.dt.trajectory.TrajectoryObsDatasetFactory;

import ucar.unidata.data.BadDataException;

import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataUtil;

import ucar.unidata.data.point.*;
import ucar.unidata.geoloc.Bearing;

import ucar.unidata.util.JobManager;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import ucar.visad.UtcDate;

import ucar.visad.Util;
import ucar.visad.quantities.*;

import visad.*;

import visad.georef.*;

import visad.util.DataUtility;


import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


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
     * @param dataSource _more_
     * @param filename  location of file
     * @param pointDataFilter _more_
     * @param stride _more_
     * @param lastNMinutes _more_
     *
     * @throws Exception    On badness
     */
    //    public CdmTrackAdapter(String filename) throws Exception {
    //        super(filename);
    //    }


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
        TrajectoryObsDataset tod = null;
        StringBuilder         buf = new StringBuilder();
        try {
            //tod = TrajectoryObsDatasetFactory.open(filename);
            tod = (TrajectoryObsDataset) TypedDatasetFactory.open(
                FeatureType.TRAJECTORY, filename, null, buf);
        } catch (java.io.FileNotFoundException fnfe) {
            throw new BadDataException("Could not open data source:"
                                       + filename);
        }

        if (tod == null) {
            throw new BadDataException("Could not open trajectory file:"
                                       + filename);
        }

        List trajectories = tod.getTrajectories();
        for (int i = 0; i < trajectories.size(); i++) {
            addTrackInfo(
                new CdmTrackInfo(
                    this, tod, (TrajectoryObsDatatype) trajectories.get(i)));
        }
    }




}

