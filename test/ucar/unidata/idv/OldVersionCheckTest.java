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

package ucar.unidata.idv;


import org.junit.Test;

import ucar.unidata.idv.OldVersionCheck.IDVVersion;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * The Class OldVersionCheckTest.
 */
public class OldVersionCheckTest {


    /**
     * Test to see of the IDV current version from Unidata is being obtained
     * properly.
     */
    @Test
    public void testGetCurrentVersion() {
        assertTrue(OldVersionCheck.getCurrentIDVVersion() != null);
    }


    /**
     * Test IDV Version equality
     */
    @Test
    public void testIdvVersionEquality() {
        IDVVersion idv1 = new IDVVersion("4", "0", "u", "1");
        IDVVersion idv2 = new IDVVersion("4", "0", "u", "1");
        assertTrue(idv1.equals(idv1));
        assertTrue(idv1.equals(idv2));
    }

    /**
     * Test IDV version comparison
     */
    @Test
    public void testIdvVersionComparison() {
        IDVVersion idv1 = new IDVVersion("4", "0", "b", "2");
        IDVVersion idv2 = new IDVVersion("4", "0", "u", "1");
        IDVVersion idv3 = new IDVVersion("4", "0", "u", "2");
        IDVVersion idv4 = new IDVVersion("5", "0", "u", "1");
        assertTrue(idv2.compareTo(idv1) < 0);
        assertTrue(idv3.compareTo(idv2) < 0);
        assertTrue(idv4.compareTo(idv3) < 0);
    }



    /**
     * Test parse IDV Version String
     */
    @Test
    public void testParse() {

        final String msg = "IDV Version not parsing correctly";

        assertEquals(msg, "5.0", new IDVVersion("5.0").toString());
        assertEquals(msg, "2.7u1", new IDVVersion("2.7u1").toString());
        assertEquals(msg, "2.7u1", new IDVVersion("2.7 u1").toString());
        assertEquals(msg, "2.11u42", new IDVVersion("2.11u42").toString());
        assertEquals(msg, "3.0b1", new IDVVersion("3.0b1").toString());
        assertEquals(msg, "2.7uX", new IDVVersion("2.7uX").toString());
    }


    /**
     * Test parse IDV Version String
     */
    @Test
    public void testIdvVersion() {
        final String msg = "IDV Version incorrect";
        assertEquals(msg, "2.6",
                     new IDVVersion("2", "6", null, null).toString());
        assertEquals(msg, "2.6", new IDVVersion("2", "6", "", "").toString());
        assertEquals(msg, "2.6",
                     new IDVVersion("2", "6", " ", " ").toString());
        assertEquals(msg, "2.7u2",
                     new IDVVersion("2", "7", "u", "2").toString());
        assertEquals(msg, "2.8uX",
                     new IDVVersion("2", "8", "u", "X").toString());
        assertEquals(msg, "3.0b1",
                     new IDVVersion("3", "0", "b", "1").toString());
    }

    /**
     * Test comparable implementation
     */
    @Test
    public void testSort() {
        final List<IDVVersion> list = new LinkedList<IDVVersion>();
        list.add(new IDVVersion("2.4"));
        list.add(new IDVVersion("2.5"));
        list.add(new IDVVersion("2.4"));
        list.add(new IDVVersion("2.5"));
        list.add(new IDVVersion("2.6"));
        list.add(new IDVVersion("2.6uX"));
        list.add(new IDVVersion("2.7"));
        list.add(new IDVVersion("2.7u2"));
        list.add(new IDVVersion("2.7uX"));
        list.add(new IDVVersion("2.8"));
        list.add(new IDVVersion("2.8uX"));
        list.add(new IDVVersion("2.9"));
        list.add(new IDVVersion("2.9u1"));
        list.add(new IDVVersion("2.9u2"));
        list.add(new IDVVersion("2.9u3"));
        list.add(new IDVVersion("3.0"));
        list.add(new IDVVersion("3.0b1"));
        list.add(new IDVVersion("3.0u1"));
        list.add(new IDVVersion("3.0u2"));
        list.add(new IDVVersion("3.1"));
        list.add(new IDVVersion("3.1u1"));
        list.add(new IDVVersion("3.1u2"));
        list.add(new IDVVersion("4.0"));
        list.add(new IDVVersion("4.0u1"));

        for (int j = 0; j < 100; j++) {
            Collections.shuffle(list);
            Collections.sort(list);
            StringBuilder sb = new StringBuilder();
            for (IDVVersion i : list) {
                sb.append(i.toString() + ",");
            }
            assertEquals(
                "IDV Version not sorting correctly",
                "4.0u1,4.0,3.1u2,3.1u1,3.1,3.0u2,3.0u1,3.0b1,3.0,2.9u3,2.9u2,2.9u1,2.9,2.8uX,2.8,2.7uX,2.7u2,2.7,2.6uX,2.6,2.5,2.5,2.4,2.4,",
                sb.toString());

        }
    }

    /**
     * Test comparable implementation
     */
    @Test
    public void parseOutVersion() {
        final String msg = "IDV Version not parsing correctly";
        assertEquals(msg, OldVersionCheck.getIDVVersion().toString(),
                     OldVersionCheck.parseOutVersion("").toString());
        assertEquals(
            msg, OldVersionCheck.getIDVVersion().toString(),
            OldVersionCheck.parseOutVersion("fjhkfjhfdkjh").toString());
        assertEquals(
            msg, "2.6",
            OldVersionCheck.parseOutVersion(
                "xxxxxxxxxx Unidata IDV API v2.6 xxxxxxxxxxx").toString());
        assertEquals(
            msg, "2.6uX",
            OldVersionCheck.parseOutVersion(
                "xxxxxxxxxx Unidata IDV API v2.6uX xxxxxxxxxxx").toString());
        assertEquals(
            msg, "2.7u2",
            OldVersionCheck.parseOutVersion(
                "xxxxxxxxxx Unidata IDV API v2.7u2 xxxxxxxxxxx").toString());
        assertEquals(
            msg, "3.0b1",
            OldVersionCheck.parseOutVersion(
                "xxxxxxxxxx Unidata IDV API v3.0b1 xxxxxxxxxxx").toString());
        assertEquals(
            msg, "4.0u1",
            OldVersionCheck.parseOutVersion(
                "xxxxxxxxxx Unidata IDV API v4.0u1 xxxxxxxxxxx").toString());

    }
}
