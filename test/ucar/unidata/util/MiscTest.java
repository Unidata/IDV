/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
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

package ucar.unidata.util;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;


/**
 * The tests for the Misc class.
 */
public class MiscTest {


    /**
     * Test the Misc.decodeLatLon method.
     */
    @Test
    public void testDecodeLatLonsString() {

        /** The Constant DELTA. */
        final double                                          DELTA = 0.001;

        @SuppressWarnings("serial") final Map<String, Double> map   =
            new HashMap<String, Double>() {
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
            assertEquals("Could not properly decode lat / lon",
                         Misc.decodeLatLon(e.getKey()), e.getValue(), DELTA);
        }
    }
    
    /**
     *     DD:MM:SS      ===>  -34:29:45
     *       (if longitude and use360 ===> 326:29:45)
     *     DDH           ===>   34W     (or 34S if longitude)
     *     DD.d          ===>  -34.5
     *     DD.dddH       ===>   34.496W (or 34.496S if longitude)
     *     DD MM" SS.s'  ===>  -34 29" 45.6'
     *
     */


    /**
     * Test the Misc.formatLatOrLon method.
     */
    @Test
    public void formatLatOrLon() {
        String errorMsg = "Could not properly format lat / lon";

        assertEquals(errorMsg, "12", Misc.formatLatOrLon(12, "DD", true, true));

        assertEquals(errorMsg, "12:00",
                     Misc.formatLatOrLon(12, "DD:MM", true, true));

        assertEquals(errorMsg, "12:00:00",
                     Misc.formatLatOrLon(12, "DD:MM:SS", true, true));

        assertEquals(errorMsg, "12.0",
                     Misc.formatLatOrLon(12, "DD.d", true, true));

        assertEquals(errorMsg, "12.0:00",
                     Misc.formatLatOrLon(12, "DD.d:MM", true, true));

        assertEquals(errorMsg, "12N",
                Misc.formatLatOrLon(12, "DDH", true, true));

        assertEquals(errorMsg, "12S",
                Misc.formatLatOrLon(-12, "DDH", true, true));

        assertEquals(errorMsg, "12E",
                Misc.formatLatOrLon(12, "DDH", false, true));

        assertEquals(errorMsg, "12W",
                Misc.formatLatOrLon(-12, "DDH", false, true));


        // lat > 360 should result in exception or normalization
        assertEquals(errorMsg, "10",
                     Misc.formatLatOrLon(370, "DD.d", true, true));

        //Should result in no change or exception.
        assertEquals(errorMsg, "12",
                     Misc.formatLatOrLon(12, "blah", true, true));
    }

}
