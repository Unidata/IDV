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

package ucar.unidata.xml;


import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Test;


/**
 * Testing the XMLEncoder class.
 */
public class XmlEncoderTest {

    /**
     * Testing XML deserialization of {@link java.util.Map Maps}.
     */
    @Test
    public void testMap() {
        Map<String, String> testMap = new HashMap<String, String>(3);
        testMap.put("1", "a");
        testMap.put("2", "ab");
        testMap.put("3", "abc");
        XmlEncoder encoder = new XmlEncoder();
        String     xml     = encoder.toXml(testMap);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(xml);
        Object object = encoder.toObject(encoder.toElement(testMap));
        assertEquals(testMap, object);
    }

    /**
     * Testing XML deserialization of {@link java.util.Set Sets}.
     */
    @Test
    public void testSet() {
        Set<String> testSet = new HashSet<String>(3);
        testSet.add("a");
        testSet.add("ab");
        testSet.add("abc");
        XmlEncoder encoder = new XmlEncoder();
        String     xml     = encoder.toXml(testSet);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(xml);
        Object object = encoder.toObject(encoder.toElement(testSet));
        assertEquals(testSet, object);
    }

    /**
     * Testing XML (de)serialization of a class that has an enum.
     */
    @Test
    public void testEnum() {
        TestClass1 c = new TestClass1();
        c.setDay(Day.SUNDAY);
        XmlEncoder encoder = new XmlEncoder();
        String     xml     = encoder.toXml(c);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(xml);
        Object object = encoder.toObject(encoder.toElement(c));
        assertEquals(c, object);
    }


    /**
     * Testing list property methods
     */
    @Test
    public void testListPropertyMethods() {
        TestClass2 c = new TestClass2();
        assertEquals(2, XmlEncoder.findPropertyMethods(c.getClass()).size());
    }


    /**
     * Test class 1.
     */
    public static class TestClass1 {

        /** The day. */
        public Day day;

        /**
         * Instantiates a new test class 1.
         */
        public TestClass1() {}

        /**
         * Gets the day.
         *
         * @return the day
         */
        public Day getDay() {
            return day;
        }

        /**
         * Sets the day.
         *
         * @param day the new day
         */
        public void setDay(Day day) {
            this.day = day;
        }

        /**
         * {@inheritDoc}
         *
         */
        @Override
        public int hashCode() {
            final int prime  = 31;
            int       result = 1;
            result = prime * result + ((day == null)
                                       ? 0
                                       : day.hashCode());
            return result;
        }


        /**
         * {@inheritDoc}
         *
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TestClass1 other = (TestClass1) obj;
            if (day != other.day) {
                return false;
            }
            return true;
        }

    }

    /**
     * Test class 2.
     */
    public static class TestClass2 {

        /** The boolean b. */
        private boolean b;

        /** The i. */
        private int i;



        /**
         * Instantiates a new test class 2.
         */
        public TestClass2() {}

        /**
         * Gets the b.
         *
         * @return the b
         */
        public boolean isB() {
            return b;
        }

        /**
         * Sets the b.
         *
         * @param b the new b
         */
        public void setB(boolean b) {
            this.b = b;
        }

        /**
         * Gets the i.
         *
         * @return the i
         */
        public int getI() {
            return i;
        }

        /**
         * Sets the i.
         *
         * @param i the new i
         */
        public void setI(int i) {
            this.i = i;
        }
    }


    /**
     * The Enum Day.
     */
    public enum Day {

        /** SUNDAY. */
        SUNDAY,

        /** MONDAY. */
        MONDAY,

        /** TUESDAY. */
        TUESDAY,

        /** WEDNESDAY. */
        WEDNESDAY,

        /** THURSDAY. */
        THURSDAY,

        /** FRIDAY. */
        FRIDAY,

        /** SATURDAY. */
        SATURDAY
    }
}
