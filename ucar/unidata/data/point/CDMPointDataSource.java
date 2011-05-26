package ucar.unidata.data.point;

//~--- non-JDK imports --------------------------------------------------------

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.geoloc.LatLonRect;

import visad.FieldImpl;
import visad.VisADException;

/**
 * The Class CDMPointDataSource.
 */
public class CDMPointDataSource extends PointDataSource {
    
    /**
     * Instantiates a new cDM point data source.
     *
     * @throws VisADException the vis ad exception
     */
    public CDMPointDataSource() throws VisADException {
        super();
    }

    /* (non-Javadoc)
     * @see ucar.unidata.data.point.PointDataSource#makeObs(ucar.unidata.data.DataChoice, ucar.unidata.data.DataSelection, ucar.unidata.geoloc.LatLonRect)
     */
    @Override
    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset, LatLonRect bbox) throws Exception {
        return null;
    }
}
