package ucar.unidata.data.sounding;

//~--- non-JDK imports --------------------------------------------------------

import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import ucar.ma2.StructureData;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.StationProfileFeatureCollection;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.point.PointDataSource;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import visad.FieldImpl;
import visad.VisADException;

/**
 * The Class CDMPointDataSource.
 */
public class CDMProfileDataSource extends PointDataSource {
    
    

	/**
     * Instantiates a new cDM point data source.
     *
     * @throws VisADException the vis ad exception
     */
    public CDMProfileDataSource() throws VisADException {
        super();
    }

    /* (non-Javadoc)
     * @see ucar.unidata.data.point.PointDataSource#makeObs(ucar.unidata.data.DataChoice, ucar.unidata.data.DataSelection, ucar.unidata.geoloc.LatLonRect)
     */
    @Override
    protected FieldImpl makeObs(final DataChoice dataChoice, final DataSelection subset, final LatLonRect bbox) throws Exception {
    	final Formatter formatter = new Formatter(new StringBuffer(), Locale.US);
    	//Obviously will have to be parameterized
    	final FeatureDatasetPoint dataset = (FeatureDatasetPoint)FeatureDatasetFactoryManager.open(FeatureType.STATION_PROFILE, "/tmp/Upperair_20110526_0000.nc", null, formatter);
    	for (FeatureCollection fc : dataset.getPointFeatureCollectionList()) {
			//Eventually, Logic to pull apart feature collection and put into a FieldImpl will be found here.
    		final StationProfileFeatureCollection spfc = (StationProfileFeatureCollection)fc;
    		for (Station station : spfc.getStations()) {
    			StationProfileFeature stationProfileFeature = spfc.getStationProfileFeature(station);
    			for (Date date : stationProfileFeature.getTimes()) {
    				ProfileFeature profileByDate = stationProfileFeature.getProfileByDate(date);
    				while (profileByDate.hasNext()) {
    					PointFeature pf = profileByDate.next();
    					StructureData data = pf.getData();
    				}
				}
    			//Iterate through all times
			}
    		
		}
    	
        return null;
    }
}
