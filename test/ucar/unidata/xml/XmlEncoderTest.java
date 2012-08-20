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

package ucar.unidata.xml;


import org.junit.Test;


import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;


/**
 * Testing the XMLEncoder class.
 */
public class XmlEncoderTest {

    /**
     * Testing XML (de)serialization of a class that has an enum.
     */
    @Test
    public void testEnum() {
        TestClass c = new TestClass();
        c.setDay(Day.SUNDAY);
        XmlEncoder encoder = new XmlEncoder();
        String     xml     = encoder.toXml(c);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(xml);
        Object object = encoder.toObject(encoder.toElement(c));
        assertEquals(c, object);
    }

    /**
     * The Class Foo.
     */
    public static class TestClass {

        /** The day. */
        public Day day;

        /**
         * Instantiates a new foo.
         */
        public TestClass() {}

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
            TestClass other = (TestClass) obj;
            if (day != other.day) {
                return false;
            }
            return true;
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
