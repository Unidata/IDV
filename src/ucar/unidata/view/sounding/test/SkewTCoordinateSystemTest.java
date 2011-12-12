/*
 * $Id: SkewTCoordinateSystemTest.java,v 1.4 2005/05/13 18:33:43 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.view.sounding.test;



import junit.framework.*;

import junit.textui.TestRunner;

import visad.*;

import ucar.unidata.view.sounding.*;


/**
 * Tests the Skew-T coordinate-system transformation.
 *
 * @author Steven R. Emmerson
 */
public class SkewTCoordinateSystemTest extends TestCase {

    /** default CoordinateSystem */
    private SkewTCoordinateSystem defaultCS;

    /**
     * Constructs from a name.
     *
     * @param name           The name.
     */
    public SkewTCoordinateSystemTest(String name) {
        super(name);
    }

    /**
     * Sets-up the test.
     */
    protected void setUp() {

        try {
            defaultCS = SkewTCoordinateSystem.instance();
        } catch (VisADException e) {
            fail(e.toString());
        }
    }

    /**
     * Tests the default, upper-left corner.
     */
    public void testDefaultUpperLeftCorner() {

        /**
         * try
         * {
         *   float       minP = (float)defaultCS.getMinimumPressure().getValue();
         *   assert(minP > 0);
         *   float       minT =
         *       (float)defaultCS.getMinimumTemperature().getValue();
         *   assert(minT != 0);
         *   float[][]   cornerXY =
         *       defaultCS.toReference(new float[][] {{minP}, {minT}});
         *   assert(cornerXY.length == 2);
         *   assert(cornerXY[0].length == 1);
         *   assert(cornerXY[0][0] < 0);
         *   assert(cornerXY[0][0] >= -1);
         *   assert(areClose(cornerXY[1].length, 1));
         *   assert(cornerXY[1][0] > 0);
         *   assert(cornerXY[1][0] <= 1);
         *   float[][]   cornerPT = defaultCS.fromReference(cornerXY);
         *   assert(cornerPT.length == 2);
         *   assert(areClose(cornerPT[0].length, 1));
         *   assert(areClose(cornerPT[0][0], minP));
         *   assert(areClose(cornerPT[1].length, 1));
         *   assert(areClose(cornerPT[1][0], minT));
         * }
         * catch (VisADException e)
         * {
         *   fail(e.toString());
         * }
         */
    }

    /**
     * Tests the default, lower-right corner.
     */
    public void testDefaultLowerRightCorner() {

        /**
         * try
         * {
         *   float       maxP = (float)defaultCS.getMaximumPressure().getValue();
         *   assert(maxP > 0);
         *   float       maxT =
         *       (float)defaultCS.getMaximumTemperature().getValue();
         *   assert(maxT != 0);
         *   float[][]   cornerXY =
         *       defaultCS.toReference(new float[][] {{maxP}, {maxT}});
         *   assert(cornerXY.length == 2);
         *   assert(cornerXY[0].length == 1);
         *   assert(cornerXY[0][0] > 0);
         *   assert(areClose(cornerXY[0][0], 1));
         *   assert(cornerXY[1].length == 1);
         *   assert(cornerXY[1][0] < 0);
         *   assert(cornerXY[1][0] >= -1);
         *   float[][]   cornerPT = defaultCS.fromReference(cornerXY);
         *   assert(cornerPT.length == 2);
         *   assert(cornerPT[0].length == 1);
         *   assert(areClose(cornerPT[0][0], maxP));
         *   assert(cornerPT[1].length == 1);
         *   assert(areClose(cornerPT[1][0], maxT));
         * }
         * catch (VisADException e)
         * {
         *   fail(e.toString());
         * }
         */
    }

    /**
     * Tests the equals() method.
     */
    public void testEquals() {

        /**
         * assert(!defaultCS.equals(null));
         * try
         * {
         *   SkewTCoordinateSystem       cs = SkewTCoordinateSystem.instance();
         *   assert(defaultCS.equals(cs));
         *   cs = SkewTCoordinateSystem.instance(
         *       new RealTupleType(
         *           new RealType[] {RealType.YAxis, RealType.XAxis}));
         *   assert(!defaultCS.equals(cs));
         * }
         * catch (VisADException e)
         * {
         *   fail(e.toString());
         * }
         */
    }

    /**
     * Tests the hashCode() method.
     */
    public void testHashCode() {

        /**
         * try
         * {
         *   assert(
         *       defaultCS.hashCode() ==
         *       SkewTCoordinateSystem.instance().hashCode());
         * }
         * catch (VisADException e)
         * {
         *   fail(e.toString());
         * }
         */
    }

    /**
     * Returns a test suite.
     *
     * @return             A test suite.
     */
    public static Test suite() {
        return new TestSuite(SkewTCoordinateSystemTest.class);
    }

    /**
     * Executes the test.
     *
     * @param args          Invocation arguments.  Ignored.
     */
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    /**
     * See if two values are almost the same
     *
     * @param a   first value
     * @param b   second value
     * @return  true if close
     */
    private boolean areClose(double a, double b) {

        double absDiff = Math.abs(a - b);

        return ((absDiff == 0) || (a == 0))
               ? absDiff < 1e-6
               : (b == 0)
                 ? absDiff < 1e-6
                 : absDiff / Math.max(Math.abs(a), Math.abs(b)) < 1e-6;
    }
}







