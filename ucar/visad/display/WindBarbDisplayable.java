/*
 * $Id: WindBarbDisplayable.java,v 1.14 2006/11/26 15:24:56 dmurray Exp $
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



package ucar.visad.display;


import ucar.visad.WindBarb;


import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.java2d.*;

import visad.util.DataUtility;



import java.rmi.RemoteException;

import java.util.Iterator;


/**
 * Provides support for a Displayable to show wind with the
 * conventional meteorological "wind barb" symbols.
 *
 * @author Don Murray and Stuart Wier
 * @version $Revision: 1.14 $
 */
public class WindBarbDisplayable extends FlowDisplayable {

    /**
     * Constructs from a name for the Displayable and the type of the
     * parameter.
     *
     * @param name           The name for the displayable.
     * @param rTT        The VisAD RealTupleType of the parameter.  May be
     *                          <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindBarbDisplayable(String name, RealTupleType rTT)
            throws VisADException, RemoteException {
        super(name, rTT, 0.1f);
    }

    /**
     * Returns the {@link visad.DataRenderer} associated with this instance.
     *
     * @return             The {@link visad.DataRenderer} associated with this
     *                     instance.
     */
    protected DataRenderer getDataRenderer() {
        return (getDisplay().getDisplayRenderer()
                instanceof DisplayRendererJ2D)
               ? (DataRenderer) new visad.bom.BarbRendererJ2D()
        //: (DataRenderer) new visad.bom.BarbRendererJ3D();
               : (DataRenderer) new MyBarbRenderer();
    }

    /**
     * Sets the RealType of the RGB parameter.  Override so that
     * we set the units to be KNOTS since wind barbs are always
     * in knots
     * @param realType          The RealType of the RGB parameter.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setRGBRealType(RealType realType)
            throws RemoteException, VisADException {
        super.setRGBRealType(realType);
        setDisplayUnit(CommonUnits.KNOT);
    }


    /**
     * Set the range of the flow maps
     *
     * @param min min value
     * @param max max value
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setFlowRange(double min, double max)
            throws VisADException, RemoteException {

        if (isCartesianWind()) {
            flowXMap.setRange(-1.0, 1.0);
            flowYMap.setRange(-1.0, 1.0);
        } else {
            flowXMap.setRange(0.0, 360.0);
            flowYMap.setRange(0.0, 1.0);
        }
    }

    /**
     * Check to see if this is 3D flow
     * @return  false
     */
    public boolean get3DFlow() {
        return false;
    }

    /**
     * Custom barb renderer
     *
     *
     * @author IDV Development Team
     */
    protected static class MyBarbRenderer extends visad.bom.BarbRendererJ3D {

        /**
         * Ctor
         */
        public MyBarbRenderer() {
            super();
        }

        /**
         * Make the barb
         *
         * @param south true if southern hemisphere
         * @param x  x position
         * @param y  y position
         * @param z  z position
         * @param scale  scale factor
         * @param pt_size  spacing
         * @param f0  u component
         * @param f1  v component
         * @param vx  line x points
         * @param vy  line y points
         * @param vz  line z points
         * @param numv  num line points
         * @param tx triangle x points
         * @param ty triangle y points
         * @param tz triangle z points
         * @param numt triangles
         *
         * @return stuff
         */
        public float[] makeVector(boolean south, float x, float y, float z,
                                  float scale, float pt_size, float f0,
                                  float f1, float[] vx, float[] vy,
                                  float[] vz, int[] numv, float[] tx,
                                  float[] ty, float[] tz, int[] numt) {
            return WindBarb.makeBarb(south, x, y, z, scale, pt_size, f0, f1,
                                     vx, vy, vz, numv, tx, ty, tz, numt);
        }

        /**
         * Clone this
         *
         * @return a new one
         */
        public Object clone() {
            return new MyBarbRenderer();
        }

    }
}

