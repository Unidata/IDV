/*
 * $Id: SkewTDisplay.java,v 1.28 2005/05/13 18:33:37 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import java.awt.Dimension;
import java.awt.event.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;

import javax.swing.*;

import ucar.unidata.beans.*;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;
import visad.data.netcdf.Plain;

import visad.java2d.*;

import visad.java3d.*;


/**
 * Provides support for a 2-D VisAD display for a Skew T, Log P Diagram
 * (alias "Skew-T Chart").
 *
 * @author Unidata Development Team
 * @version $Id: SkewTDisplay.java,v 1.28 2005/05/13 18:33:37 jeffmc Exp $
 */
public class SkewTDisplay extends AerologicalDisplay {

    /**
     * The type of the display pressure.
     */
    private static DisplayRealType displayPressureType = null;

    /**
     * The type of the display temperature.
     */
    private static DisplayRealType displayTemperatureType = null;

    /**
     * The type of the (dummy) Z-axis.
     */
    private static DisplayRealType displayZAxisType = null;

    /**
     * The (Pressure,Temperature,Z) display space.
     */
    private static DisplayTupleType displayTupleType = null;

    /**
     * Constructs the default instance.  The default instance is based on the
     * default Skew-T coordinate-system transformation and the unit square in
     * (Display.XAxis,Display.YAxis) space.
     *
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException if a Java RMI failure occurs.
     * @see SkewTCoordinateSystem
     */
    public SkewTDisplay() throws VisADException, RemoteException {
        this(SkewTCoordinateSystem.instance());
    }

    /**
     * Constructs from a Skew-T coordinate-system transformation.
     *
     * @param coordinateSystem          The coordinate-system transformation for
     *                                  the Skew-T chart.
     * @throws UnitException            Incompatible units.
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException          if a Java RMI failure occurs.
     */
    public SkewTDisplay(SkewTCoordinateSystem coordinateSystem)
            throws UnitException, VisADException, RemoteException {

        super(new DisplayImplJ2D("Skew-T Chart"), 6, null, null,
              coordinateSystem);

        ((ProjectionControl) getDisplay().getProjectionControl()).setAspect(
            new double[]{ 1.2,
                          1.2 });
        saveProjection();

    }

    /**
     * Returns the type of the display pressure.
     * @return                  The type of the display pressure.
     * @throws VisADException   VisAD failure.
     */
    public static DisplayRealType getDisplayPressureType()
            throws VisADException {
        return displayPressureType;
    }

    /**
     * Returns the type of the display temperature.
     * @return                  The type of the display temperature.
     * @throws VisADException   VisAD failure.
     */
    public static DisplayRealType getDisplayTemperatureType()
            throws VisADException {
        return displayTemperatureType;
    }

    /**
     * Tests this class.
     * @param args              The test arguments.
     * @throws Exception        Something went wrong.
     */
    public static void main(String[] args) throws Exception {

        String             pathName     = (args.length > 0)
                                          ? args[0]
                                          : "sounding.nc";
        final SkewTDisplay skewTDisplay = new SkewTDisplay();

        skewTDisplay.draw();

        try {
            FlatField field = (FlatField) new Plain().open(pathName);

            skewTDisplay
                .addProfile((Field) Util
                    .ensureMathType((FlatField) field
                        .extract(0), new FunctionType(AirPressure
                            .getRealTupleType(), AirTemperature
                            .getRealType())), (Field) Util
                                .ensureMathType((FlatField) field
                                    .extract(1), new FunctionType(AirPressure
                                        .getRealTupleType(), DewPoint
                                        .getRealType())));
            skewTDisplay.setProfileVisible(0, true);
        } catch (Exception e) {
            ;
        }

        JFrame jframe = new JFrame("Skew-T Chart");

        jframe.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        jframe.getContentPane().add(skewTDisplay.getComponent());
        jframe.pack();

        Dimension screenSize = jframe.getToolkit().getScreenSize();
        Dimension frameSize  = jframe.getSize();

        jframe.setLocation((screenSize.width - frameSize.width) / 2,
                           (screenSize.height - frameSize.height) / 2);
        jframe.setVisible(true);

        // Thread.sleep(2000);
        //skewTDisplay.setPointMode(true);
    }

    /**
     * Returns the minimum, displayed, X-value
     * @return                  The minimum, displayed, X-value
     */
    protected Real getMinimumX() {
        return getCoordinateSystem().getMinimumX();
    }

    /**
     * Returns the maximum, displayed, X-value
     * @return                  The maximum, displayed, X-value
     */
    protected Real getMaximumX() {
        return getCoordinateSystem().getMaximumX();
    }

    /**
     * Returns the minimum, displayed, Y-value
     * @return                  The minimum, displayed, Y-value
     */
    protected Real getMinimumY() {
        return getCoordinateSystem().getMinimumY();
    }

    /**
     * Returns the maximum, displayed, Y-value
     * @return                  The maximum, displayed, Y-value
     */
    protected Real getMaximumY() {
        return getCoordinateSystem().getMaximumY();
    }

    /**
     * Returns the minimum, displayed, pressure.
     * @return                  The minimum, displayed, pressure.
     */
    public Real getMinimumPressure() {
        return getCoordinateSystem().getMinimumPressure();
    }

    /**
     * Returns the maximum, displayed, pressure.
     * @return                  The maximum, displayed, pressure.
     */
    public Real getMaximumPressure() {
        return getCoordinateSystem().getMaximumPressure();
    }

    /**
     * Returns the minimum, displayed, temperature.
     * @return                  The minimum, displayed, temperature.
     */
    public Real getMinimumTemperature() {
        return getCoordinateSystem().getMinimumTemperature();
    }

    /**
     * Returns the maximum, displayed, temperature.
     * @return                  The maximum, displayed, temperature.
     */
    public Real getMaximumTemperature() {
        return getCoordinateSystem().getMaximumTemperature();
    }

}
