
/**
 * $Id: StuveDisplay.java,v 1.6 2005/05/13 18:33:39 jeffmc Exp $
 *
 * Copyright  1997-2002 Unidata Program Center/University Corporation for
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
 *
 */

package ucar.unidata.view.sounding;


import java.awt.Dimension;
import java.awt.BorderLayout;
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
 * Provides support for a 2-D VisAD display for a T, -p (Stuve) diagram where
 * p is -p**k (k = R/CP).
 *
 * @author Unidata Development Team
 * @version $Id: StuveDisplay.java,v 1.6 2005/05/13 18:33:39 jeffmc Exp $
 */
public class StuveDisplay extends AerologicalDisplay {


    /**
     * Constructs the default instance.  The default instance is based on the
     * default Stuve coordinate-system transformation and the unit square in
     * (Display.XAxis,Display.YAxis) space.
     *
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException if a Java RMI failure occurs.
     * @see StuveCoordinateSystem
     */
    public StuveDisplay() throws VisADException, RemoteException {
        this(StuveCoordinateSystem.instance());
    }

    /**
     * Constructs from a Stuve coordinate-system transformation.
     *
     * @param coordinateSystem          The coordinate-system transformation for
     *                                  the Stuve chart.
     * @throws UnitException            Incompatible units.
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException          if a Java RMI failure occurs.
     */
    public StuveDisplay(StuveCoordinateSystem coordinateSystem)
            throws UnitException, VisADException, RemoteException {

        super(coordinateSystem);

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
        final StuveDisplay stuveDisplay = new StuveDisplay();

        stuveDisplay.draw();

        try {
            FlatField field = (FlatField) new Plain().open(pathName);

            stuveDisplay
                .addProfile((Field) Util
                    .ensureMathType((FlatField) field
                        .extract(0), new FunctionType(AirPressure
                            .getRealTupleType(), AirTemperature
                            .getRealType())), (Field) Util
                                .ensureMathType((FlatField) field
                                    .extract(1), new FunctionType(AirPressure
                                        .getRealTupleType(), DewPoint
                                        .getRealType())));
            stuveDisplay.setProfileVisible(0, true);
        } catch (Exception e) {}

        JFrame jframe = new JFrame("Stuve Chart");

        jframe.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        jframe.getContentPane().add(stuveDisplay.getComponent());
        JButton change = new JButton("Change to Emagram");
        change.addActionListener(new ActionListener() {
            public boolean stuve = true;

            public void actionPerformed(ActionEvent e) {
                JButton button = (JButton) e.getSource();
                try {
                    stuveDisplay.setCoordinateSystem((stuve)
                                                     ? (AerologicalCoordinateSystem) EmagramCoordinateSystem
                                                         .instance()
                                                     : (AerologicalCoordinateSystem) StuveCoordinateSystem
                                                         .instance());
                    stuve = !stuve;
                    button.setText(stuve
                                   ? "Change to Emagram"
                                   : "Change to Stuve");
                } catch (Exception excp) {
                    excp.printStackTrace();
                }
            }
        });
        jframe.getContentPane().add(change, BorderLayout.SOUTH);
        jframe.pack();

        Dimension screenSize = jframe.getToolkit().getScreenSize();
        Dimension frameSize  = jframe.getSize();

        jframe.setLocation((screenSize.width - frameSize.width) / 2,
                           (screenSize.height - frameSize.height) / 2);
        jframe.setVisible(true);

        // Thread.sleep(2000);
        //stuveDisplay.setPointMode(true);
    }

}
