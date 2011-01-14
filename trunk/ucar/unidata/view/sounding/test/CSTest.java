/*
 * $Id: CSTest.java,v 1.5 2005/05/13 18:33:43 jeffmc Exp $
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



import java.awt.event.*;

import javax.swing.*;

import visad.*;
import visad.java2d.*;

import ucar.unidata.view.sounding.*;


/**
 * Class CSTest
 *
 *
 * @author Unidata development team
 * @version %I%, %G%
 */
class CSTest extends JPanel {

    /**
     * Main method for testing
     *
     * @param args  not used
     *
     * @throws Exception  problem with code
     */
    public static void main(String[] args) throws Exception {

        DisplayRendererJ2D displayRenderer = new DefaultDisplayRendererJ2D() {

            public boolean legalDisplayScalar(DisplayRealType displayType) {
                return true;
            }
        };
        DisplayImplJ2D display = new DisplayImplJ2D("CSTest",
                                                    displayRenderer);
        RealType rangeType = new RealType("range");
        FlatField contours =
            new FlatField(
                new FunctionType(
                    RealTupleType
                        .SpatialCartesian2DTuple, rangeType), new Linear2DSet(
                            RealTupleType
                                .SpatialCartesian2DTuple, -1, 1, 3, -1, 1, 3));

        contours.setSamples(new float[][] {
            {
                -0, -1, -2, 1, 0, -1, 2, 1, 0
            }
        });
        display.addMap(new ScalarMap(RealType.XAxis, Display.XAxis));
        display.addMap(new ScalarMap(RealType.YAxis, Display.YAxis));
        display.addMap(new ScalarMap(rangeType, Display.IsoContour));

        RealType xType = new RealType("x");
        RealType yType = new RealType("y");
        FlatField path = new FlatField(new FunctionType(xType, yType),
                                       new Linear1DSet(xType, -1, 1, 3));

        path.setSamples(new float[][] {
            { 1, 0, -1 }
        });

        DisplayRealType displayX = new DisplayRealType("displayX", true, -1,
                                       1, 0, (Unit) null);
        DisplayRealType displayY = new DisplayRealType("displayY", true, -1,
                                       1, 0, (Unit) null);
        DisplayRealType displayZ = new DisplayRealType("displayZ", true, 0,
                                       (Unit) null);
        DisplayTupleType displayTuple =
            new DisplayTupleType(new DisplayRealType[]{ displayX,
                                                        displayY,
                                                        displayZ }, new DisplayCS(
                                                            false));

        display.addMap(new ScalarMap(xType, displayX));
        display.addMap(new ScalarMap(yType, displayY));

        DataReference contoursRef = new DataReferenceImpl("contoursRef");

        contoursRef.setData(contours);
        display.addReference(contoursRef);

        DataReference pathRef = new DataReferenceImpl("pathRef");

        pathRef.setData(path);
        display.addReferences(new DirectManipulationRendererJ2D(), pathRef);

        JFrame jframe = new JFrame("SetDisplay");

        jframe.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        jframe.getContentPane().add(display.getComponent());
        jframe.pack();
        jframe.setVisible(true);
        Thread.sleep(2000);
        display.removeAllReferences();
        display.clearMaps();
        displayX.setTuple((DisplayTupleType)null, -1, false);
        displayY.setTuple((DisplayTupleType)null, -1, false);
        displayZ.setTuple((DisplayTupleType)null, -1, false);

        displayTuple = new DisplayTupleType(new DisplayRealType[]{ displayX,
                                                                   displayY,
                                                                   displayZ }, new DisplayCS(
                                                                   true));

        display.addMap(new ScalarMap(RealType.XAxis, Display.XAxis));
        display.addMap(new ScalarMap(RealType.YAxis, Display.YAxis));
        display.addMap(new ScalarMap(rangeType, Display.IsoContour));
        display.addMap(new ScalarMap(xType, displayX));
        display.addMap(new ScalarMap(yType, displayY));
        display.addReference(contoursRef);
        display.addReferences(new DirectManipulationRendererJ2D(), pathRef);
    }

    /**
     * Class DataCS
     *
     * @author IDV development team
     */
    protected static class DataCS extends CoordinateSystem {

        /**
         * Default ctor
         *
         * @throws VisADException
         *
         */
        public DataCS() throws VisADException {
            super(RealTupleType.SpatialCartesian2DTuple, null);
        }

        /**
         * Transform from reference
         *
         * @param values   xy values
         * @return  values
         */
        public double[][] fromReference(double[][] values) {
            return values;
        }

        /**
         * Transform to reference
         *
         * @param values   values to transform
         * @return values
         */
        public double[][] toReference(double[][] values) {
            return values;
        }

        /**
         * Check for equality.
         *
         * @param obj  Object to check
         * @return  true if equal
         */
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    /**
     * Class DisplayCS
     *
     *
     * @author IDV development team
     */
    protected static class DisplayCS extends CoordinateSystem {

        /** change flag */
        private boolean change;

        /**
         * Construct a new Display CS
         *
         * @param change  true to change
         *
         * @throws VisADException  problem with change
         *
         */
        public DisplayCS(boolean change) throws VisADException {

            super(Display.DisplaySpatialCartesianTuple, null);

            this.change = change;
        }

        /**
         * Transform from reference coords
         *
         * @param values   values to transform
         * @return  transformed values
         */
        public double[][] fromReference(double[][] values) {

            if (change) {
                for (int i = 0; i < values.length; ++i) {
                    for (int j = 0; j < values[i].length; ++j) {
                        values[i][j] *= 2;
                    }
                }
            }

            return values;
        }

        /**
         * Transform to reference coords
         *
         * @param values   values to transform
         * @return  transformed values
         */
        public double[][] toReference(double[][] values) {

            if (change) {
                for (int i = 0; i < values.length; ++i) {
                    for (int j = 0; j < values[i].length; ++j) {
                        values[i][j] /= 2;
                    }
                }
            }

            return values;
        }

        /**
         * Check for equality.
         *
         * @param obj  Object to check
         * @return  true if equal
         */
        public boolean equals(Object obj) {
            return obj == this;
        }
    }
}
