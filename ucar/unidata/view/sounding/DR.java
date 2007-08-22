/*
 * $Id: DR.java,v 1.10 2005/05/13 18:33:27 jeffmc Exp $
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



import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.rmi.RemoteException;

import javax.swing.JFrame;

import visad.DataReferenceImpl;

import visad.Display;

import visad.DisplayImpl;

import visad.DisplayRealType;

import visad.DisplayTupleType;

import visad.Gridded2DSet;

import visad.RealType;

import visad.RealTupleType;

import visad.SampledSet;

import visad.ScalarMap;

import visad.UnionSet;

import visad.VisADException;

import visad.java2d.DefaultDisplayRendererJ2D;
import visad.java2d.DisplayImplJ2D;


/**
 * Provides a 2-D VisAD display for a Skew T, Log P Diagram (alias "Skew-T
 * Chart").  Instances of this class use a protected coordinate system that maps
 * between (PRESSURE,TEMPERATURE) and (Display.XAxis,Display.YAxis).
 *
 * @author Steven R. Emmerson
 * @version $Id: DR.java,v 1.10 2005/05/13 18:33:27 jeffmc Exp $
 */
public class DR extends DefaultDisplayRendererJ2D {

    /**
     * Constructs from nothing.
     */
    public DR() {}

    /**
     * Adds the background.
     * @throws VisADException if a core VisAD failure occurs.
     */
    public void enableBackground() throws VisADException {

        /*
         * Add background contours.
         */
        {
            DataReferenceImpl ref = new DataReferenceImpl("YContours");

            try {
                ref.setData(newYContours());
                getDisplay().addReference(ref);
            } catch (RemoteException e) {}  // can't happen because newPressureContours() is local
        }
    }

    /**
     * Returns new Y contours.
     * @return                New Y contours.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected UnionSet newYContours() throws VisADException {

        SampledSet[] sets = new SampledSet[9];
        float        y    = -0.8f;

        for (int i = 0; i < sets.length; ++i) {
            sets[i] =
                new Gridded2DSet(new DisplayTupleType(new DisplayRealType[]{
                    Display.XAxis,
                    Display.YAxis }), new float[][] {
                new float[]{ -1, 1 }, new float[]{ y, y }
            }, 2);
            y += .2;
        }

        return new UnionSet(sets);
    }

    /**
     * Tests this class.
     *
     * @param args              Execution arguments.
     * @throws Exception        Something went wrong.
     */
    public static void main(String[] args) throws Exception {

        JFrame jframe = new JFrame("Test");

        jframe.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        DR displayRenderer = new DR();
        DisplayImpl display = new DisplayImplJ2D(jframe.getTitle(),
                                                 displayRenderer);

        display.addMap(new ScalarMap(Display.XAxis, Display.XAxis));
        display.addMap(new ScalarMap(Display.YAxis, Display.YAxis));
        displayRenderer.enableBackground();
        jframe.getContentPane().add(display.getComponent());
        jframe.pack();
        jframe.setVisible(true);
    }
}







