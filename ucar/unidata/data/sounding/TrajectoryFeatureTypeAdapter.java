package ucar.unidata.data.sounding;

import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.TrajectoryFeatureCollection;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.data.BadDataException;

import java.util.Hashtable;
import java.util.Formatter;
import java.util.List;

import visad.Data;

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
    public TrajectoryFeatureTypeAdapter(TrackDataSource dataSource, String filename,
                           Hashtable pointDataFilter, int stride,
                           int lastNMinutes)
            throws Exception {
        super(dataSource, filename, pointDataFilter, stride, lastNMinutes);


        Formatter log = new Formatter();
        FeatureDatasetPoint dataset  = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.TRAJECTORY, filename, null, log);

        if (dataset == null) {
            throw new BadDataException("Could not open trajectory file:"
                                       + filename);
        }
        List<FeatureCollection> fcList = dataset.getPointFeatureCollectionList();
        FeatureCollection fc = fcList.get(0);
        TrajectoryFeatureCollection pfc = (TrajectoryFeatureCollection) fc;
        pfc.resetIteration();


        // we can add difference trajFeatureTypeInfos here from difference data source
        addTrackInfo( new CosmicTrajectoryFeatureTypeInfo(
                     this, dataset, pfc));
        dataset.close();

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

        CosmicTrajectoryFeatureTypeInfo ctfi = (CosmicTrajectoryFeatureTypeInfo) getTrackInfo(trackId);
        return ctfi.getAerologicalDiagramData();
    }

}
