package ucar.unidata.view.geoloc;

//~--- non-JDK imports --------------------------------------------------------

import org.junit.Test;

import ucar.unidata.view.geoloc.CoordinateFormat.Cardinality;
import ucar.unidata.view.geoloc.CoordinateFormat.DecimalCoordFormat;
import ucar.unidata.view.geoloc.CoordinateFormat.DegMinSec;
import ucar.unidata.view.geoloc.CoordinateFormat.FloorCoordFormat;

import static org.junit.Assert.assertEquals;

import static ucar.unidata.view.geoloc.CoordinateFormat.EMPTY_FORMAT;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;

/**
 * The Class CoordinateFormatTest.
 */
public class CoordinateFormatTest {

    /**
     * Test decode lat lons string.
     */
    @Test
    public void testDecodeLatLonsString() {
        @SuppressWarnings("serial") final Map<String, String> map = new HashMap<String, String>() {
            {
                put("0¡",
                    CoordinateFormat.convert(0, new DecimalCoordFormat(0, DegMinSec.DEGREE), EMPTY_FORMAT,
                                             EMPTY_FORMAT, Cardinality.NONE));
                put("51¡N",
                    CoordinateFormat.convert(51.4605876, new DecimalCoordFormat(0, DegMinSec.DEGREE), EMPTY_FORMAT,
                                             EMPTY_FORMAT, Cardinality.NORTH));
                put("51¡28'N",
                    CoordinateFormat.convert(51.4605876, new FloorCoordFormat(DegMinSec.DEGREE),
                                             new DecimalCoordFormat(0, DegMinSec.MINUTE), EMPTY_FORMAT,
                                             Cardinality.NORTH));
                put("51¡27.635'N",
                    CoordinateFormat.convert(51.4605876, new FloorCoordFormat(DegMinSec.DEGREE),
                                             new DecimalCoordFormat(3, DegMinSec.MINUTE), EMPTY_FORMAT,
                                             Cardinality.NORTH));
                put("51¡27'38\"N",
                    CoordinateFormat.convert(51.4605876, new FloorCoordFormat(DegMinSec.DEGREE),
                                             new FloorCoordFormat(DegMinSec.MINUTE),
                                             new DecimalCoordFormat(0, DegMinSec.SECOND), Cardinality.NORTH));
                put("51:28N",
                    CoordinateFormat.convert(51.4605876, new FloorCoordFormat(DegMinSec.COLON),
                                             new DecimalCoordFormat(0, DegMinSec.NONE), EMPTY_FORMAT,
                                             Cardinality.NORTH));
                put("51:27:38N",
                    CoordinateFormat.convert(51.4605876, new FloorCoordFormat(DegMinSec.COLON),
                                             new FloorCoordFormat(DegMinSec.COLON),
                                             new DecimalCoordFormat(0, DegMinSec.NONE), Cardinality.NORTH));
                put("51:27:38.11536N",
                    CoordinateFormat.convert(51.4605876, new FloorCoordFormat(DegMinSec.COLON),
                                             new FloorCoordFormat(DegMinSec.COLON),
                                             new DecimalCoordFormat(5, DegMinSec.NONE), Cardinality.NORTH));
                put("51.46059",
                    CoordinateFormat.convert(51.4605876, new DecimalCoordFormat(5, DegMinSec.NONE), EMPTY_FORMAT,
                                             EMPTY_FORMAT, Cardinality.NONE));
            }
        };

        for (Map.Entry<String, String> e : map.entrySet()) {
            assertEquals("Coordinate conversion incorrect.", e.getKey(), e.getValue());
        }
    }
}
