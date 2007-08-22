/*
 * $Id: PseudoAdiabaticTrajectoryTest.java,v 1.4 2005/05/13 18:33:43 jeffmc Exp $
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

import ucar.visad.quantities.*;

import visad.*;


import ucar.unidata.view.sounding.*;


/**
 * Tests the pseudo-adiabatic trajectory capability of this package.
 *
 * @author Steven R. Emmerson
 */
public class PseudoAdiabaticTrajectoryTest extends TestCase {

    /**
     * Constructs from a name.
     *
     * @param name         The name.
     */
    public PseudoAdiabaticTrajectoryTest(String name) {
        super(name);
    }

    /**
     * Tests instance construction.
     */
    public void testInstanceConstruction() {

        try {
            PseudoAdiabaticTrajectory trajectory =
                PseudoAdiabaticTrajectory
                    .instance(new Real(AirPressure
                        .getRealType(), 1000, CommonUnits
                        .MILLIBAR), new Real(AirTemperature
                        .getRealType(), 30, CommonUnits
                        .CELSIUS), new Real(DewPoint
                        .getRealType(), 19, CommonUnits
                        .CELSIUS), new Real(AirPressure
                        .getRealType(), 100, CommonUnits.MILLIBAR));

            System.out.println(trajectory.toString());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Return a test suite.
     *
     * @return          A test suite.
     */
    public static Test suite() {
        return new TestSuite(PseudoAdiabaticTrajectoryTest.class);
    }

    /**
     * Run this test.
     *
     * @param args        Invocation arguments.  Ignored.
     */
    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}







