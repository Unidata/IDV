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
package ucar.unidata.data.sounding;


import ucar.nc2.constants.FeatureType;

import ucar.nc2.ft.*;

import ucar.unidata.data.BadDataException;

import visad.Data;

import java.util.Formatter;

import java.util.Hashtable;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Oct 5, 2009
 * Time: 2:44:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrajectoryFeatureTypeAdapter extends TrackAdapter {


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
    private boolean isCosmic = false;

    /**
     * Construct a new track from the filename
     *
     *
     * @param dataSource _more_
     * @param filename  location of file
     * @param pointDataFilter Filters the variables to use
     * @param stride The stride
     * @param lastNMinutes use the last N minutes
     *
     * @throws Exception    On badness
     */
    public TrajectoryFeatureTypeAdapter(TrackDataSource dataSource,
                                        String filename,
                                        Hashtable pointDataFilter,
                                        int stride, int lastNMinutes)
            throws Exception {
        super(dataSource, filename, pointDataFilter, stride, lastNMinutes);


        Formatter log = new Formatter();
        FeatureDatasetPoint dataset =
            (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.TRAJECTORY, filename, null, log);
        String imp = dataset.getImplementationName();
        if (dataset == null) {
            throw new BadDataException("Could not open trajectory file:"
                                       + filename);
        }
        List<FeatureCollection> fcList =
            dataset.getPointFeatureCollectionList();
        FeatureCollection           fc  = fcList.get(0);
        TrajectoryFeatureCollection pfc = (TrajectoryFeatureCollection) fc;
        pfc.resetIteration();


        // we can add difference trajFeatureTypeInfos here from difference data source
        if (imp.equalsIgnoreCase("cosmic")) {
            isCosmic = true;
            addTrackInfo(new CosmicTrajectoryFeatureTypeInfo(this, dataset,
                    pfc));
        } else {
            addTrackInfo(new CDMTrajectoryFeatureTypeInfo(this, dataset,
                    pfc));
        }
        dataset.close();

    }

    /**
     * _more_
     *
     * @param isC _more_
     */
    public void setIsCosmic(boolean isC) {
        this.isCosmic = isC;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsCosmic() {
        return isCosmic;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isCosmic() {
        return isCosmic;
    }

    /**
     * Get the track data parameters necessary to plot an aerological
     * diagram. Returned data is of type:
     * <pre>
     *     (Time -> (AirPressure,
     *                AirTemperature,
     *                Dewpoint,
     *                (Latitude, Longitude, Altitude)))
     * </pre>
     *
     *
     * @param trackId id of the track
     * @return Data object in the format above
     *
     *
     * @throws Exception On badness
     */
    public Data getAerologicalDiagramData(String trackId) throws Exception {

        CosmicTrajectoryFeatureTypeInfo ctfi =
            (CosmicTrajectoryFeatureTypeInfo) getTrackInfo(trackId);
        return ctfi.getAerologicalDiagramData();
    }

}
