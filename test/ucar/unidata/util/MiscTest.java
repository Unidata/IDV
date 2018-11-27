/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The tests for the Misc class.
 */
public class MiscTest {

    /**
     * Test the Misc.decodeLatLon method.
     */
    @Test
    public void testMakeUnique() {
        List<Integer> listDup    = Arrays.asList(1, 3, 4, 5, 1);
        List<Integer> listUnique = Misc.makeUnique(listDup);
        String        error      = "Misc.makeUnique not working";
        assertEquals(error, listUnique.size(), 4);

        listDup    = Arrays.asList(1, 3, null, 5, 1);
        listUnique = Misc.makeUnique(listDup);
        assertEquals(error, listUnique.size(), 4);

        listUnique = Misc.makeUnique(Collections.<Integer>emptyList());
        assertEquals(error, listUnique.size(), 0);
    }


    /**
     * Test the Misc.decodeLatLon method.
     */
    @Test
    public void testDecodeLatLonsString() {

        /** The Constant DELTA. */
        final double DELTA = 0.001;

        @SuppressWarnings("serial") final Map<String, Double> map =
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
     * Test the Misc.formatLatitude method.
     */
    @Test
    public void formatLat() {
        String errorMsg = "Could not properly format lat";

        assertEquals(errorMsg, "12", Misc.formatLatitude(12, "DD"));

        //Testing various formats
        assertEquals(errorMsg, "12:00", Misc.formatLatitude(12, "DD:MM"));

        assertEquals(errorMsg, "12:00:00",
                     Misc.formatLatitude(12, "DD:MM:SS"));

        assertEquals(errorMsg, "12.0", Misc.formatLatitude(12, "DD.d"));

        assertEquals(errorMsg, "12 30'", Misc.formatLatitude(12.5, "DD MM'"));

        assertEquals(errorMsg, "12 33' 18\"",
                     Misc.formatLatitude(12.555, "DD MM' SS\""));

        assertEquals(errorMsg, "12 33' 19.8\"",
                     Misc.formatLatitude(12.5555, "DD MM' SS.s\""));

        assertEquals(errorMsg, "-12 33' 19.8\"",
                     Misc.formatLatitude(-12.5555, "DD MM' SS.s\""));

        assertEquals(errorMsg, "-0 06' 00.0\"",
                     Misc.formatLatitude(-0.1, "DD MM' SS.s\""));

        //Testing cardinalities
        assertEquals(errorMsg, "12N", Misc.formatLatitude(12, "DDH"));

        assertEquals(errorMsg, "12", Misc.formatLatitude(12, "DD"));

        assertEquals(errorMsg, "12S", Misc.formatLatitude(-12, "DDH"));

        assertEquals(errorMsg, "-12", Misc.formatLatitude(-12, "DD"));

        //Boundary case testing
        assertEquals(errorMsg, "0", Misc.formatLatitude(0, "DD"));

        assertEquals(errorMsg, "0 00' 00.0\"",
                     Misc.formatLatitude(0, "DDH MM' SS.s\""));
    }

    /**
     * Test the Misc.formatLongitude method.
     */
    @Test
    public void formatLongitude() {
        String errorMsg = "Could not properly format lon";

        //Testing cardinalities        
        assertEquals(errorMsg, "12E", Misc.formatLongitude(12, "DDH", false));

        assertEquals(errorMsg, "12", Misc.formatLongitude(12, "DD", false));

        assertEquals(errorMsg, "12W",
                     Misc.formatLongitude(-12, "DDH", false));

        assertEquals(errorMsg, "-12", Misc.formatLongitude(-12, "DD", false));

        //testing 0-360
        assertEquals(errorMsg, "348", Misc.formatLongitude(-12, "DDH", true));

        assertEquals(errorMsg, "12", Misc.formatLongitude(12, "DDH", true));

        assertEquals(errorMsg, "348", Misc.formatLongitude(-12, "DD", true));

        assertEquals(errorMsg, "12", Misc.formatLongitude(12, "DD", true));

        //Boundary case testing

        assertEquals(errorMsg, "180E",
                     Misc.formatLongitude(180, "DDH", false));

        assertEquals(errorMsg, "180", Misc.formatLongitude(180, "DD", false));

        assertEquals(errorMsg, "180W",
                     Misc.formatLongitude(-180, "DDH", false));

        assertEquals(errorMsg, "-180",
                     Misc.formatLongitude(-180, "DD", false));

        //testing 0-360
        assertEquals(errorMsg, "180",
                     Misc.formatLongitude(-180, "DDH", true));

        assertEquals(errorMsg, "180", Misc.formatLongitude(180, "DDH", true));

        assertEquals(errorMsg, "0", Misc.formatLongitude(0, "DDH", false));

        assertEquals(errorMsg, "0", Misc.formatLongitude(0, "DD", false));

        //testing 0-360
        assertEquals(errorMsg, "0", Misc.formatLongitude(0, "DDH", true));


    }
}
