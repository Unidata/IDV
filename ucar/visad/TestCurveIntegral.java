/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.visad;


import visad.*;

import visad.java2d.DisplayImplJ2D;



import java.rmi.RemoteException;


/**
 * Tests the {@link VisADMath#curveIntegralOfGradient} method.
 *
 * @author Steven R. Emmerson
 */
public class TestCurveIntegral extends UISkeleton {

    /**
     * Constructs from nothing.
     */
    public TestCurveIntegral() {}

    /**
     * Constructs from a array of string arguments.
     *
     * @param args             The arguments.
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public TestCurveIntegral(String args[])
            throws VisADException, RemoteException {
        super(args);
    }

    /**
     * Set up the data for this test
     * @return  displays for test
     *
     * @throws RemoteException if a Java RMI failure occurs.
     * @throws VisADException  if a core VisAD failure occurs.
     */
    DisplayImpl[] setupData() throws VisADException, RemoteException {

        int       domain_flag = 0;
        int       LengthX     = 201;
        int       LengthY     = 201;
        int       n_samples   = LengthX * LengthY;
        int       ii, jj;
        int       index;
        FlatField d_field;
        Set       domainSet = null;
        RealType  x_axis    = RealType.getRealType("x_axis", SI.meter, null);
        RealType  y_axis    = RealType.getRealType("y_axis", SI.meter, null);
        MathType  Domain    = (MathType) new RealTupleType(x_axis, y_axis);
        MathType rangeTemp = (MathType) RealType.getRealType("Temperature",
                                 SI.kelvin, null);
        FunctionType domain_temp = new FunctionType(Domain, rangeTemp);

        if (domain_flag == 0) {
            domainSet = (Set) new Linear2DSet(Domain, 0.d, 1000.d, LengthX,
                    0.d, 1000.d, LengthY);
        } else if (domain_flag == 1) {
            float[][] d_samples = new float[2][n_samples];

            index = 0;

            for (ii = 0; ii < LengthY; ii++) {
                for (jj = 0; jj < LengthX; jj++) {
                    d_samples[0][index] = jj * 5f;
                    d_samples[1][index] = ii * 5f;

                    index++;
                }
            }

            domainSet = (Set) new Gridded2DSet(Domain, d_samples, LengthX,
                    LengthY, null, null, null);
        } else if (domain_flag == 3) {}

        FlatField  f_field = new FlatField(domain_temp, domainSet);
        double[][] samples = new double[1][n_samples];

        index = 0;

        double wave_number = 2;
        double PI          = java.lang.Math.PI;

        for (ii = 0; ii < LengthY; ii++) {
            for (jj = 0; jj < LengthX; jj++) {
                samples[0][index] = (50)
                                    * java.lang.Math.sin(
                                        ((wave_number * 2d * PI) / 1000) * 5
                                        * jj) * java.lang.Math.sin(
                                            ((wave_number * 2d * PI) / 1000)
                                            * 5 * ii);

                index++;
            }
        }

        f_field.setSamples(samples);
        System.out.println("Starting derivative computation...");

        Tuple tuple = (Tuple) f_field.derivative(Data.NO_ERRORS);

        System.out.println("...derivative done");

        d_field = (FlatField) FieldImpl.combine(new Field[] {
            (Field) tuple.getComponent(0),
            (Field) tuple.getComponent(1) });

        long time = System.currentTimeMillis();

        System.out.println(
            "Starting indefinite curve integral computation...");

        d_field = VisADMath.curveIntegralOfGradient(d_field);
        time    = System.currentTimeMillis() - time;

        long memory = Runtime.getRuntime().totalMemory();

        System.out.println("...indefinite curve integral done; time=" + time
                           + "; memory=" + memory);

        MathType    f_range  = ((FunctionType) d_field.getType()).getRange();
        DisplayImpl display1 = new DisplayImplJ2D("display1");

        display1.addMap(new ScalarMap((RealType) x_axis, Display.XAxis));
        display1.addMap(new ScalarMap((RealType) y_axis, Display.YAxis));
        display1.addMap(new ScalarMap((RealType) rangeTemp, Display.Green));
        display1.addMap(new ConstantMap(0.5, Display.Red));
        display1.addMap(new ConstantMap(0.5, Display.Blue));

        /**
         * ScalarMap map1contour;
         * map1contour = new ScalarMap( (RealType)rangeTemp, Display.IsoContour );
         * display1.addMap( map1contour );
         * ContourControl control1contour;
         * control1contour = (ContourControl) map1contour.getControl();
         *
         * control1contour.enableContours(true);
         * control1contour.enableLabels(false);
         */
        GraphicsModeControl mode = display1.getGraphicsModeControl();

        mode.setScaleEnable(true);

        DisplayImpl display2 = new DisplayImplJ2D("display2");

        display2.addMap(new ScalarMap((RealType) x_axis, Display.XAxis));
        display2.addMap(new ScalarMap((RealType) y_axis, Display.YAxis));
        display2.addMap(new ScalarMap((RealType) f_range, Display.Green));
        display2.addMap(new ConstantMap(0.5, Display.Red));
        display2.addMap(new ConstantMap(0.5, Display.Blue));

        /**
         * map1contour = new ScalarMap( (RealType)f_range, Display.IsoContour );
         * display2.addMap( map1contour );
         * control1contour = (ContourControl) map1contour.getControl();
         *
         * control1contour.enableContours(true);
         * control1contour.enableLabels(false);
         */
        mode = display2.getGraphicsModeControl();

        mode.setScaleEnable(true);

        DataReferenceImpl ref_imaget1 = new DataReferenceImpl("ref_imaget1");

        ref_imaget1.setData(f_field);
        display1.addReference(ref_imaget1, null);

        DataReferenceImpl ref_imaget2 = new DataReferenceImpl("ref_imaget2");

        ref_imaget2.setData(d_field);
        display2.addReference(ref_imaget2, null);

        DisplayImpl[] dpys = new DisplayImpl[2];

        dpys[0] = display1;
        dpys[1] = display2;

        return dpys;
    }

    /**
     * Get the frame title
     * @return  title for the frame
     */
    String getFrameTitle() {
        return "sinusoidal field    and    (d/dx)field";
    }

    /**
     * Returns a string representation of this instance.  This
     * implementation returns a string of dubious value.
     *
     * @return            a string representation of this instance.
     */
    public String toString() {
        return ": Function.derivative with Linear2DSet in Java2D";
    }

    /**
     * Tests this class.
     *
     * @param args             The invocation arguments.
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public static void main(String args[])
            throws VisADException, RemoteException {
        TestCurveIntegral t = new TestCurveIntegral(args);
    }
}
