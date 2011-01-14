/*
 * $Id: Test.java,v 1.8 2006/12/01 20:42:32 jeffmc Exp $
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

package ucar.unidata.data.gis;


import ucar.unidata.geoloc.projection.*;

import ucar.visad.*;

import visad.*;

import visad.bom.*;

import visad.data.*;
import visad.data.gif.*;
import visad.data.jai.*;
import visad.data.mcidas.BaseMapAdapter;
import visad.data.netcdf.Plain;
import visad.data.tiff.*;

import visad.georef.*;

import visad.java3d.*;

import visad.jmet.GRIBCoordinateSystem;



import java.awt.event.*;

import javax.swing.*;



/**
 * Test routine for GeoTIFF
 *
 * @author IDV Development team
 * @version $Revision: 1.8 $
 */
public class Test {

    /**
     * Create a new Test from the source
     *
     * @param source   file name
     *
     */
    public Test(String source) {}


    /**
     * Test this routine
     *
     * @param args   filename
     *
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Must supply a filename");
            System.exit(1);
        }

        GeotiffAdapter  adapter = new GeotiffAdapter(args[0]);
        final FlatField data    = adapter.getData();
        RealTupleType rangeType =
            ((FunctionType) data.getType()).getFlatRange();
        DisplayImpl display =
        /*
            new DisplayImplJ2D("display");
            new DisplayImplJ3D("display", new TwoDDisplayRendererJ3D());
        */
        new DisplayImplJ3D("display");
        /*
        GraphicsModeControl gmc = display.getGraphicsModeControl();
        gmc.setTextureEnable(false);
        */

        boolean flat = true;
        if (flat) {
            display.addMap(new ScalarMap(RealType.Latitude, Display.YAxis));
            display.addMap(new ScalarMap(RealType.Longitude, Display.XAxis));
        } else {
            display.addMap(new ScalarMap(RealType.Latitude,
                                         Display.Latitude));
            display.addMap(new ScalarMap(RealType.Longitude,
                                         Display.Longitude));
        }
        display.addMap(new ScalarMap((RealType) rangeType.getComponent(0),
                                     Display.Red));
        display.addMap(new ScalarMap((RealType) rangeType.getComponent(1),
                                     Display.Green));
        display.addMap(new ScalarMap((RealType) rangeType.getComponent(2),
                                     Display.Blue));
        DataReference ref = new DataReferenceImpl("ref");
        ref.setData(data);
        display.addReference(ref);
        /*
        try {
            BaseMapAdapter bma = new BaseMapAdapter("OUTLSUPW");
            DataReference mapRef = new DataReferenceImpl("map");
            mapRef.setData(bma.getData());
            display.addReference(mapRef);
        } catch (Exception e) {
            System.out.println("Map OUTLSUPW not available");
        }
        */
        JFrame frame = new JFrame("adapter test");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.getContentPane().add(display.getComponent());
        JButton save = new JButton("Save");
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    new Plain().save("world.nc", data, true);
                } catch (Exception exp) {
                    System.out.println("couldn't save image");
                }
            }
        });
        frame.getContentPane().add("South", save);
        frame.pack();
        frame.show();
    }
}

