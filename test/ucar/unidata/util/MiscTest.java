package ucar.unidata.util;

//~--- non-JDK imports --------------------------------------------------------

import org.junit.Test;

import static org.junit.Assert.assertEquals;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;

/**
 * The JUnit test class MiscTest.
 */
public class MiscTest {

    /** The Constant DELTA. */
    private static final double DELTA = 0.001;

    /**
     * testDecodeLatLonsString.
     */
    @Test
    public void testDecodeLatLonsString() {
        @SuppressWarnings("serial") final Map<String, Double> map = new HashMap<String, Double>() {
            {
                put("4:25:W", -4.416666666);
                put("-9E-3", -0.0090);
                put("4:25:E", 4.416666666);
                put("65:02:06", 65.035);
                put("-147:30:06", -147.50166);
                put("31.77", 31.77);
                put("-95.71", -95.71);
                put("53:26:N", 53.4333);
                put("8:20:S", -8.3333);
            }
        };

        for (Map.Entry<String, Double> e : map.entrySet()) {
            assertEquals("Could not properly decode lat / lon", Misc.decodeLatLon(e.getKey()), e.getValue(), DELTA);
        }
    }
}
