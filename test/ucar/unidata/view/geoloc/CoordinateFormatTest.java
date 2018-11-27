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

package ucar.unidata.view.geoloc;


import org.junit.Test;

import ucar.unidata.view.geoloc.CoordinateFormat.Cardinality;
import ucar.unidata.view.geoloc.CoordinateFormat.DecimalCoordFormat;
import ucar.unidata.view.geoloc.CoordinateFormat.DegMinSec;
import ucar.unidata.view.geoloc.CoordinateFormat.FloorCoordFormat;


import static org.junit.Assert.assertEquals;

import static ucar.unidata.view.geoloc.CoordinateFormat.EMPTY_FORMAT;

import java.util.HashMap;
import java.util.Map;


/**
 * The Class CoordinateFormatTest.
 */
public class CoordinateFormatTest {

    /**
     * Test CoordinateFormat.convert
     */
    @Test
    public void testConvert() {
        @SuppressWarnings("serial") final Map<String, String> map =
            new HashMap<String, String>() {
            {
                put("00 ",
                    CoordinateFormat.convert(0, new DecimalCoordFormat(0,
                        DegMinSec.DEGREE), EMPTY_FORMAT, EMPTY_FORMAT,
                                           Cardinality.NONE));
                put("51 N",
                    CoordinateFormat.convert(
                        51.4605876,
                        new DecimalCoordFormat(0, DegMinSec.DEGREE),
                        EMPTY_FORMAT, EMPTY_FORMAT, Cardinality.NORTH));
                put("51 28'N",
                    CoordinateFormat.convert(
                        51.4605876, new FloorCoordFormat(DegMinSec.DEGREE),
                        new DecimalCoordFormat(0, DegMinSec.MINUTE),
                        EMPTY_FORMAT, Cardinality.NORTH));
                put("51 27.635'N",
                    CoordinateFormat.convert(
                        51.4605876, new FloorCoordFormat(DegMinSec.DEGREE),
                        new DecimalCoordFormat(3, DegMinSec.MINUTE),
                        EMPTY_FORMAT, Cardinality.NORTH));
                put("51 27'38\"N",
                    CoordinateFormat.convert(
                        51.4605876, new FloorCoordFormat(DegMinSec.DEGREE),
                        new FloorCoordFormat(DegMinSec.MINUTE),
                        new DecimalCoordFormat(0, DegMinSec.SECOND),
                        Cardinality.NORTH));
                put("51:28N",
                    CoordinateFormat.convert(
                        51.4605876, new FloorCoordFormat(DegMinSec.COLON),
                        new DecimalCoordFormat(0, DegMinSec.NONE),
                        EMPTY_FORMAT, Cardinality.NORTH));
                put("51:27:38N",
                    CoordinateFormat.convert(
                        51.4605876, new FloorCoordFormat(DegMinSec.COLON),
                        new FloorCoordFormat(DegMinSec.COLON),
                        new DecimalCoordFormat(0, DegMinSec.NONE),
                        Cardinality.NORTH));
                put("51:27:38.11536N",
                    CoordinateFormat.convert(
                        51.4605876, new FloorCoordFormat(DegMinSec.COLON),
                        new FloorCoordFormat(DegMinSec.COLON),
                        new DecimalCoordFormat(5, DegMinSec.NONE),
                        Cardinality.NORTH));
                put("12:33:19.80N",
                    CoordinateFormat.convert(
                        12.5555, new FloorCoordFormat(DegMinSec.COLON),
                        new FloorCoordFormat(DegMinSec.COLON),
                        new DecimalCoordFormat(2, DegMinSec.NONE),
                        Cardinality.NORTH));
                put("-12:33:19.80N",
                    CoordinateFormat.convert(
                        -12.5555, new FloorCoordFormat(DegMinSec.COLON),
                        new FloorCoordFormat(DegMinSec.COLON),
                        new DecimalCoordFormat(2, DegMinSec.NONE),
                        Cardinality.NORTH));
                put("-00:06:00",
                    CoordinateFormat.convert(
                        -0.1, new FloorCoordFormat(DegMinSec.COLON),
                        new FloorCoordFormat(DegMinSec.COLON),
                        new DecimalCoordFormat(0, DegMinSec.NONE),
                        Cardinality.NONE));
                put("51.46059",
                    CoordinateFormat.convert(
                        51.4605876,
                        new DecimalCoordFormat(5, DegMinSec.NONE),
                        EMPTY_FORMAT, EMPTY_FORMAT, Cardinality.NONE));
            }
        };

        for (Map.Entry<String, String> e : map.entrySet()) {
            assertEquals("Coordinate conversion incorrect.", e.getKey(),
                         e.getValue());
        }
    }

    /**
     * Test the CoordinateFormat.formatLatitude method.
     */
    @Test
    public void testFormatLat() {
        String errorMsg = "Could not properly format lat";

        assertEquals(errorMsg, "12",
                     CoordinateFormat.formatLatitude(12, "DD"));

        //Testing various formats
        assertEquals(errorMsg, "12:00",
                     CoordinateFormat.formatLatitude(12, "DD:MM"));

        assertEquals(errorMsg, "12:00:00",
                     CoordinateFormat.formatLatitude(12, "DD:MM:SS"));

        assertEquals(errorMsg, "12.0",
                     CoordinateFormat.formatLatitude(12, "DD.d"));

        assertEquals(errorMsg, "12 30'",
                     CoordinateFormat.formatLatitude(12.5, "DD MM'"));

        assertEquals(errorMsg, "12 33'18\"",
                     CoordinateFormat.formatLatitude(12.555, "DD MM'SS\""));

        assertEquals(errorMsg, "12 33'19.8\"",
                     CoordinateFormat.formatLatitude(12.5555,
                         "DD MM' SS.s\""));

        assertEquals(errorMsg, "-12 33'19.8\"",
                     CoordinateFormat.formatLatitude(-12.5555,
                         "DD MM'SS.s\""));

        assertEquals(errorMsg, "-00 06'00.0\"",
                     CoordinateFormat.formatLatitude(-0.1, "DD MM' SS.s\""));

        //Testing cardinalities
        assertEquals(errorMsg, "12N",
                     CoordinateFormat.formatLatitude(12, "DDH"));

        assertEquals(errorMsg, "12",
                     CoordinateFormat.formatLatitude(12, "DD"));

        assertEquals(errorMsg, "12S",
                     CoordinateFormat.formatLatitude(-12, "DDH"));

        assertEquals(errorMsg, "-12",
                     CoordinateFormat.formatLatitude(-12, "DD"));

        //Boundary case testing
        assertEquals(errorMsg, "00",
                     CoordinateFormat.formatLatitude(0, "DD"));

        assertEquals(errorMsg, "00 00'00.0\"",
                     CoordinateFormat.formatLatitude(0, "DDH MM' SS.s\""));
    }

    /**
     * Test the CoordinateFormat.formatLongitude method.
     */
    @Test
    public void testFormatLongitude() {
        String errorMsg = "Could not properly format lon";

        //Testing cardinalities        
        assertEquals(errorMsg, "12E",
                     CoordinateFormat.formatLongitude(12, "DDH", false));

        assertEquals(errorMsg, "12",
                     CoordinateFormat.formatLongitude(12, "DD", false));

        assertEquals(errorMsg, "12W",
                     CoordinateFormat.formatLongitude(-12, "DDH", false));

        assertEquals(errorMsg, "-12",
                     CoordinateFormat.formatLongitude(-12, "DD", false));

        //testing 0-360
        assertEquals(errorMsg, "348",
                     CoordinateFormat.formatLongitude(-12, "DDH", true));

        assertEquals(errorMsg, "12",
                     CoordinateFormat.formatLongitude(12, "DDH", true));

        assertEquals(errorMsg, "348",
                     CoordinateFormat.formatLongitude(-12, "DD", true));

        assertEquals(errorMsg, "12",
                     CoordinateFormat.formatLongitude(12, "DD", true));

        //Boundary case testing

        assertEquals(errorMsg, "180E",
                     CoordinateFormat.formatLongitude(180, "DDH", false));

        assertEquals(errorMsg, "180",
                     CoordinateFormat.formatLongitude(180, "DD", false));

        assertEquals(errorMsg, "180W",
                     CoordinateFormat.formatLongitude(-180, "DDH", false));

        assertEquals(errorMsg, "-180",
                     CoordinateFormat.formatLongitude(-180, "DD", false));

        //testing 0-360
        assertEquals(errorMsg, "180",
                     CoordinateFormat.formatLongitude(-180, "DDH", true));

        assertEquals(errorMsg, "180",
                     CoordinateFormat.formatLongitude(180, "DDH", true));

        assertEquals(errorMsg, "00",
                     CoordinateFormat.formatLongitude(0, "DDH", false));

        assertEquals(errorMsg, "00",
                     CoordinateFormat.formatLongitude(0, "DD", false));

        assertEquals(errorMsg, "00",
                     CoordinateFormat.formatLongitude(0, "DDH", true));

        assertEquals(errorMsg, "160W",
                CoordinateFormat.formatLongitude(200, "DDH", false));
        
        assertEquals(errorMsg, "200",
                CoordinateFormat.formatLongitude(200, "DDH", true));

    }

}
