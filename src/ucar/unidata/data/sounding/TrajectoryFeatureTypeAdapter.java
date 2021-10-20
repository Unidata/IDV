/*
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.ft.*;
import ucar.nc2.ft.DsgFeatureCollection;

import ucar.unidata.data.BadDataException;

import visad.Data;


import java.util.Formatter;
import java.util.Hashtable;
import java.util.List;


/**
 * The Class TrajectoryFeatureTypeAdapter.
 */
public class TrajectoryFeatureTypeAdapter extends TrackAdapter {


    /**
     * Construct a new track from the filename.
     *
     */
    //    public CdmTrackAdapter(String filename) throws Exception {
    //        super(filename);
    //    }
    private boolean isCosmic = false;

    /** The is profile. */
    private boolean isProfile = false;

    public TrajectoryFeatureTypeAdapter() throws Exception {
    }
    /**
     * Construct a new track from the filename.
     *
     * @param dataSource _more_
     * @param filename  location of file
     * @param pointDataFilter Filters the variables to use
     * @param stride The stride
     * @param lastNMinutes use the last N minutes
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
                FeatureType.TRAJECTORY,
                DODSNetcdfFile.canonicalURL(filename), null, log);
        if (dataset == null) {
            dataset = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.PROFILE, filename, null, log);
            if (dataset != null) {
                isProfile = true;
            }
        }
        String imp = dataset.getImplementationName();
        if (dataset == null) {
            throw new BadDataException("Could not open trajectory file:"
                                       + filename);
        }
        List<DsgFeatureCollection> fcList =
            dataset.getPointFeatureCollectionList();
        DsgFeatureCollection           fc  = fcList.get(0);
        TrajectoryFeatureCollection tfc = null;
        ProfileFeatureCollection    pfc = null;

        if ( !isProfile) {
            tfc = (TrajectoryFeatureCollection) fc;
            tfc.resetIteration();
        } else {
            pfc = (ProfileFeatureCollection) fc;
            pfc.resetIteration();
        }


        // we can add difference trajFeatureTypeInfos here from difference data source
        if (imp.equalsIgnoreCase("cosmic")) {
            isCosmic = true;
            addTrackInfo(new CosmicTrajectoryFeatureTypeInfo(this, dataset,
                    tfc));
        }  else if (isProfile){
            for (ProfileFeature pf : pfc) {
                addTrackInfo(new CDMProfileFeatureTypeInfo(this, dataset,
                        pf));
            }
         }
        /*else if (isProfile){
           addTrackInfo(new CDMProfileFeatureTypeInfo(this, dataset,
                   pfc));
       } */
        else if (dataset.getFeatureType().equals(FeatureType.TRAJECTORY)) {
            for (TrajectoryFeature traj : tfc) {
               // if(Integer.parseInt(traj.getName()) > 800 &&  Integer.parseInt(traj.getName()) < 806)
                    addTrackInfo(
                            new CDMTrajectoryFeatureTypeInfo.PointFeatureTypeInfo(
                                    this, dataset, traj));
            }
        } else {
            addTrackInfo(
                new CDMTrajectoryFeatureTypeInfo.TrajectoryFeatureTypeInfo(
                    this, dataset, tfc));
        }
        //dataset.close();

    }

    /**
     * Sets the checks if is cosmic.
     *
     * @param isC the new checks if is cosmic
     */
    public void setIsCosmic(boolean isC) {
        this.isCosmic = isC;
    }

    /**
     * Gets the checks if is cosmic.
     *
     * @return the checks if is cosmic
     */
    public boolean getIsCosmic() {
        return isCosmic;
    }

    /**
     * Checks if is cosmic.
     *
     * @return true, if is cosmic
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
