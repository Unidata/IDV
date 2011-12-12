/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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


import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.Unit;
import visad.UnitException;

import visad.bom.BarbRendererJ3D;


/**
 * Custom barb renderer
 *
 *
 * @author IDV Development Team
 */
public class WindBarbRenderer extends BarbRendererJ3D {

    /** default unit*/
    private Unit speedUnit = CommonUnit.meterPerSecond;

    /**
     * Default ctor
     */
    public WindBarbRenderer() {
        this(CommonUnit.meterPerSecond);
    }

    /**
     * Create a WindBarbRenderer with the specified 
     * speed unit for the values being displayed
     *
     * @param speedUnit  the units of the values
     */
    public WindBarbRenderer(Unit speedUnit) {
        super();
        this.speedUnit = speedUnit;
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
                              float scale, float pt_size, float f0, float f1,
                              float[] vx, float[] vy, float[] vz, int[] numv,
                              float[] tx, float[] ty, float[] tz,
                              int[] numt) {
        // WindBarb.makeBarb(New) always expects f0 and f1 to be in m/s
        Unit u = speedUnit;
        if ((u != null) && Unit.canConvert(u, CommonUnit.meterPerSecond)
                && !u.equals(CommonUnit.meterPerSecond)) {
            try {
                // convert meters per second to knots
                f0 = (float) CommonUnit.meterPerSecond.toThis(f0, u);
                f1 = (float) CommonUnit.meterPerSecond.toThis(f1, u);
            } catch (UnitException ue) {}
        }
        CoordinateSystem dcs = getDisplayCoordinateSystem();
        boolean rotateToGlobe = 
            (dcs != null && dcs instanceof visad.SphericalCoordinateSystem);
        return WindBarb.makeBarb(south, x, y, z, scale, pt_size, f0, f1,
                                    vx, vy, vz, numv, tx, ty, tz, numt, 
                                    rotateToGlobe);
    }

    /**
     * Clone this
     *
     * @return a new one
     */
    public Object clone() {
        return new WindBarbRenderer();
    }

}
