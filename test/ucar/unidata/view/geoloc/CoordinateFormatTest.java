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
    public void testDecodeLatLonsString() {
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
                put("12:33:19.8N",
                    CoordinateFormat.convert(
                        12.5555, new FloorCoordFormat(DegMinSec.COLON),
                        new FloorCoordFormat(DegMinSec.COLON),
                        new DecimalCoordFormat(5, DegMinSec.NONE),
                        Cardinality.NORTH));
                put("-12:33:19.8N",
                    CoordinateFormat.convert(
                        -12.5555, new FloorCoordFormat(DegMinSec.COLON),
                        new FloorCoordFormat(DegMinSec.COLON),
                        new DecimalCoordFormat(5, DegMinSec.NONE),
                        Cardinality.NORTH));
                put("-00:06:00",
                    CoordinateFormat.convert(
                        -0.1, new FloorCoordFormat(DegMinSec.COLON),
                        new FloorCoordFormat(DegMinSec.COLON),
                        new DecimalCoordFormat(5, DegMinSec.NONE),
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
}
