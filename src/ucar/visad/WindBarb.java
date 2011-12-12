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


import visad.CoordinateSystem;
import visad.Data;
import visad.Display;
import visad.VisADException;
import visad.VisADGeometryArray;
import visad.VisADLineArray;
import visad.VisADTriangleArray;

//- java3d
import javax.media.j3d.Transform3D;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;


/**
 * Class WindBarb to create wind barbs as shapes
 *
 *
 * @author  IDV Development Team
 * @version $Revision: 1.5 $
 */
public class WindBarb {

    /** object for syncing */
    private static Object sync = new Object();

    /** number of points */
    private static final int NUM = 256;

    /** Default constructor */
    public WindBarb() {}

    /**
     * Static method to make flow components
     *
     * @param flow_values         values of flow
     * @param flowScale           scale factors
     * @param spatial_values      spatial values
     * @param color_values        colors for flow depictions
     * @param range_select        checks for whether this is a range select
     * @param isSouth             barb orientation flag
     * @return  array of shapes as VisADGeometryArrays
     *
     * @throws VisADException
     */
    public static VisADGeometryArray[] staticMakeFlow(float[][] flow_values,
            float flowScale, float[][] spatial_values, byte[][] color_values,
            boolean[][] range_select, boolean isSouth)
            throws VisADException {

        if (flow_values[0] == null) {
            return null;
        }
        if (spatial_values[0] == null) {
            return null;
        }

        int len  = spatial_values[0].length;
        int flen = flow_values[0].length;
        int rlen = 0;

        if (range_select[0] == null) {
            rlen = len;
        } else {
            for (int j = 0; j < range_select[0].length; j++) {
                if (range_select[0][j]) {
                    rlen++;
                }
            }
        }
        if (rlen == 0) {
            return null;
        }

        // WLH 3 June 99
        boolean[] south = new boolean[len];

        for (int i = 0; i < len; i++) {
            south[i] = isSouth;
        }

        float[] vx        = new float[NUM];
        float[] vy        = new float[NUM];
        float[] vz        = new float[NUM];
        float[] tx        = new float[NUM];
        float[] ty        = new float[NUM];
        float[] tz        = new float[NUM];
        byte[]  vred      = null;
        byte[]  vgreen    = null;
        byte[]  vblue     = null;
        byte[]  valpha    = null;
        byte[]  tred      = null;
        byte[]  tgreen    = null;
        byte[]  tblue     = null;
        byte[]  talpha    = null;
        int     numColors = (color_values != null)
                            ? color_values.length
                            : 3;
        if (color_values != null) {
            vred   = new byte[NUM];
            vgreen = new byte[NUM];
            vblue  = new byte[NUM];
            if (numColors == 4) {
                valpha = new byte[NUM];
            }
            tred   = new byte[NUM];
            tgreen = new byte[NUM];
            tblue  = new byte[NUM];
            if (numColors == 4) {
                talpha = new byte[NUM];
            }
        }
        int[] numv    = { 0 };
        int[] numt    = { 0 };

        float scale   = flowScale;          // ????
        float pt_size = 0.25f * flowScale;  // ????

        // flow vector
        float f0 = 0.0f,
              f1 = 0.0f,
              f2 = 0.0f;
        for (int j = 0; j < len; j++) {
            if ((range_select[0] == null) || range_select[0][j]) {
                // NOTE - must scale to knots
                if (flen == 1) {
                    f0 = flow_values[0][0];
                    f1 = flow_values[1][0];
                    f2 = flow_values[2][0];
                } else {
                    f0 = flow_values[0][j];
                    f1 = flow_values[1][j];
                    f2 = flow_values[2][j];
                }

                if (numv[0] + NUM / 4 > vx.length) {
                    //System.out.println("numv[0] + NUM/4 > vx.length = " + numv[0] + "," + NUM/4 + "," + vx.length);
                    float[] cx = vx;
                    float[] cy = vy;
                    float[] cz = vz;
                    int     l  = 2 * vx.length;
                    vx = new float[l];
                    vy = new float[l];
                    vz = new float[l];
                    System.arraycopy(cx, 0, vx, 0, cx.length);
                    System.arraycopy(cy, 0, vy, 0, cy.length);
                    System.arraycopy(cz, 0, vz, 0, cz.length);
                    if (color_values != null) {
                        byte[] cred   = vred;
                        byte[] cgreen = vgreen;
                        byte[] cblue  = vblue;
                        byte[] calpha = valpha;
                        vred   = new byte[l];
                        vgreen = new byte[l];
                        vblue  = new byte[l];
                        if (calpha != null) {
                            valpha = new byte[l];
                        }
                        System.arraycopy(cred, 0, vred, 0, cred.length);
                        System.arraycopy(cgreen, 0, vgreen, 0, cgreen.length);
                        System.arraycopy(cblue, 0, vblue, 0, cblue.length);
                        if (calpha != null) {
                            System.arraycopy(calpha, 0, valpha, 0,
                                             calpha.length);
                        }
                    }
                }
                if (numt[0] + NUM / 4 > tx.length) {
                    float[] cx = tx;
                    float[] cy = ty;
                    float[] cz = tz;
                    int     l  = 2 * tx.length;
                    tx = new float[l];
                    ty = new float[l];
                    tz = new float[l];
                    System.arraycopy(cx, 0, tx, 0, cx.length);
                    System.arraycopy(cy, 0, ty, 0, cy.length);
                    System.arraycopy(cz, 0, tz, 0, cz.length);
                    if (color_values != null) {
                        byte[] cred   = tred;
                        byte[] cgreen = tgreen;
                        byte[] cblue  = tblue;
                        byte[] calpha = talpha;
                        tred   = new byte[l];
                        tgreen = new byte[l];
                        tblue  = new byte[l];
                        if (calpha != null) {
                            talpha = new byte[l];
                        }
                        System.arraycopy(cred, 0, tred, 0, cred.length);
                        System.arraycopy(cgreen, 0, tgreen, 0, cgreen.length);
                        System.arraycopy(cblue, 0, tblue, 0, cblue.length);
                        if (calpha != null) {
                            System.arraycopy(calpha, 0, talpha, 0,
                                             calpha.length);
                        }
                    }
                }
                int oldnv = numv[0];
                int oldnt = numt[0];
                //System.out.println("j = "+ j);
                synchronized (sync) {
                    float mbarb[] = makeBarb(south[j], spatial_values[0][j],
                                             spatial_values[1][j],
                                             spatial_values[2][j], scale,
                                             pt_size, f0, f1, vx, vy, vz,
                                             numv, tx, ty, tz, numt);

                    //        ucar.unidata.util.Misc.printArray("mbarb = ", mbarb);
                }

                int nv = numv[0];
                int nt = numt[0];
                //System.out.println("nv = " + nv + ", nt = " + nt);
                if (color_values != null) {
                    if (color_values[0].length > 1) {
                        for (int i = oldnv; i < nv; i++) {
                            vred[i]   = color_values[0][j];
                            vgreen[i] = color_values[1][j];
                            vblue[i]  = color_values[2][j];
                            if (numColors == 4) {
                                valpha[i] = color_values[3][j];
                            }
                        }
                        for (int i = oldnt; i < nt; i++) {
                            tred[i]   = color_values[0][j];
                            tgreen[i] = color_values[1][j];
                            tblue[i]  = color_values[2][j];
                            if (numColors == 4) {
                                talpha[i] = color_values[3][j];
                            }
                        }
                    } else {  // if (color_values[0].length == 1)
                        for (int i = oldnv; i < nv; i++) {
                            vred[i]   = color_values[0][0];
                            vgreen[i] = color_values[1][0];
                            vblue[i]  = color_values[2][0];
                            if (numColors == 4) {
                                valpha[i] = color_values[3][0];
                            }
                        }
                        for (int i = oldnt; i < nt; i++) {
                            tred[i]   = color_values[0][0];
                            tgreen[i] = color_values[1][0];
                            tblue[i]  = color_values[2][0];
                            if (numColors == 4) {
                                talpha[i] = color_values[3][0];
                            }
                        }
                    }
                }
            }  // end if (range_select[0] == null || range_select[0][j])
        }                     // end for (int j=0; j<len; j++)

        int nv = numv[0];
        int nt = numt[0];
        //System.out.println("nv = " + nv + ", nt = " + nt);
        if (nv == 0) {
            return null;
        }

        VisADGeometryArray[] arrays = null;
        VisADLineArray       array  = new VisADLineArray();
        array.vertexCount = nv;

        float[] coordinates = new float[3 * nv];

        int     m           = 0;
        //System.out.println("number of vertices = " + nv);
        //System.out.print("barb runs from [" + vx[nv-2] + "," + vy[nv-2] + "," + vz[nv-2] + "]");
        //System.out.println(" to [" + vx[nv-1] + "," + vy[nv-1] + "," + vz[nv-1] + "]");
        for (int i = 0; i < nv; i++) {
            coordinates[m++] = vx[i];
            coordinates[m++] = vy[i];
            coordinates[m++] = vz[i];
        }
        //System.out.println("m should be " + 3*nv + " but is " + m);
        array.coordinates = coordinates;

        byte[] colors = null;
        if (color_values != null) {
            colors = new byte[numColors * nv];
            m      = 0;
            for (int i = 0; i < nv; i++) {
                colors[m++] = vred[i];
                colors[m++] = vgreen[i];
                colors[m++] = vblue[i];
                if (numColors == 4) {
                    colors[m++] = valpha[i];
                }
            }
            array.colors = colors;
        }

        VisADTriangleArray tarray = null;
        if (nt > 0) {
            tarray             = new VisADTriangleArray();
            tarray.vertexCount = nt;

            coordinates        = new float[3 * nt];
            float[] normals = new float[3 * nt];

            m = 0;
            for (int i = 0; i < nt; i++) {
                coordinates[m++] = tx[i];
                coordinates[m++] = ty[i];
                coordinates[m++] = tz[i];
            }
            tarray.coordinates = coordinates;

            m                  = 0;
            for (int i = 0; i < nt; i++) {
                normals[m++] = 0.0f;
                normals[m++] = 0.0f;
                normals[m++] = 1.0f;
            }
            tarray.normals = normals;

            if (color_values != null) {
                colors = new byte[numColors * nt];
                m      = 0;
                for (int i = 0; i < nt; i++) {
                    colors[m++] = tred[i];
                    colors[m++] = tgreen[i];
                    colors[m++] = tblue[i];
                    if (numColors == 4) {
                        colors[m++] = talpha[i];
                    }
                }
                tarray.colors = colors;
            }
            arrays = new VisADGeometryArray[] { array, tarray };
        } else {
            arrays = new VisADGeometryArray[] { array };
        }

        return arrays;
    }


    /**
     * adapted from Justin Baker's WindBarb, which is adapted from
     *   Mark Govett's barbs.pro IDL routine
     *
     * @param south      north or south orientation flag
     * @param x          x value location
     * @param y          y value location
     * @param z          z value location
     * @param scale      scale factor
     * @param pt_size    spacing between barbs
     * @param f0         u component
     * @param f1         v component
     * @param vx         x coordinate of VisADLineArrays
     * @param vy         y coordinate of VisADLineArrays
     * @param vz         z coordinate of VisADLineArrays
     * @param numv       number of coordinates
     * @param tx         x coordinate of VisADTriangleArrays (wind flags)
     * @param ty         y coordinate of VisADTriangleArrays (wind flags)
     * @param tz         z coordinate of VisADTriangleArrays (wind flags)
     * @param numt       number of coordinates
     * @return  an array of the start and end points
     */
    public static float[] makeBarbOld(boolean south, float x, float y,
                                      float z, float scale, float pt_size,
                                      float f0, float f1, float[] vx,
                                      float[] vy, float[] vz, int[] numv,
                                      float[] tx, float[] ty, float[] tz,
                                      int[] numt) {

        float   wsp25, slant, barb, d, c195, s195;
        float   x0, y0;
        float   x1, y1, x2, y2, x3, y3;
        int     nbarb50, nbarb10, nbarb5;

        float[] mbarb = new float[4];
        mbarb[0] = x;
        mbarb[1] = y;
        //    ucar.unidata.util.Misc.printArray("mbarb begin makebarb", mbarb);

        // convert meters per second to knots
        f0 *= (3600.0 / 1853.248);
        f1 *= (3600.0 / 1853.248);

        float wnd_spd = (float) Math.sqrt(f0 * f0 + f1 * f1);
        //System.out.println("Speed = " + wnd_spd);
        int lenv = vx.length;
        int lent = tx.length;
        //System.out.println("vx.len = " + lenv);
        //System.out.println("tx.len = " + lent);
        int nv = numv[0];
        int nt = numt[0];
        //System.out.println("nv = " + nv);
        //System.out.println("nt = " + nt);

        //determine the initial (minimum) length of the flag pole
        if (wnd_spd >= 2.5) {

            wsp25 = (float) Math.max(wnd_spd + 2.5, 5.0);
            slant = 0.15f * scale;
            barb  = 0.4f * scale;
            // WLH 6 Aug 99 - barbs point the other way (duh)
            x0 = -f0 / wnd_spd;
            y0 = -f1 / wnd_spd;

            //plot the flag pole
            // lengthen to 'd = 3.0f * barb'
            // was 'd = barb' in original BOM code
            d  = 2.0f * barb;
            x1 = (x + x0 * d);
            y1 = (y + y0 * d);

            /*
                  // commented out in original BOM code
                  vx[nv] = x;
                  vy[nv] = y;
                  vz[nv] = z;
                  nv++;
                  vx[nv] = x1;
                  vy[nv] = y1;
                  vz[nv] = z;
                  nv++;
                  // g.drawLine(x,y,x1,y1);
            */

            //determine number of wind barbs needed for 10 and 50 kt winds
            nbarb50 = (int) (wsp25 / 50.f);
            nbarb10 = (int) ((wsp25 - (nbarb50 * 50.f)) / 10.f);
            nbarb5 = (int) ((wsp25 - (nbarb50 * 50.f) - (nbarb10 * 10.f))
                            / 5.f);

            //2.5 to 7.5 kt winds are plotted with the barb part way done the pole
            if (nbarb5 == 1) {
                barb  = barb * 0.6f;
                slant = slant * 0.6f;
                x1    = (x + x0 * d);
                y1    = (y + y0 * d);

                if (south) {
                    x2 = (x + x0 * (d + slant) - y0 * barb);
                    y2 = (y + y0 * (d + slant) + x0 * barb);
                } else {
                    x2 = (x + x0 * (d + slant) + y0 * barb);
                    y2 = (y + y0 * (d + slant) - x0 * barb);
                }

                vx[nv] = x1;
                vy[nv] = y1;
                vz[nv] = z;
                nv++;
                vx[nv] = x2;
                vy[nv] = y2;
                vz[nv] = z;
                nv++;
                // System.out.println("barb5 " + x1 + " " + y1 + "" + x2 + " " + y2);
                // g.drawLine(x1, y1, x2, y2);
            }

            //add a little more pole
            if ((wsp25 >= 5.0f) && (wsp25 < 10.0f)) {
                //d  = d + 0.25f * scale;
                d  = d + pt_size;
                x1 = (x + x0 * d);
                y1 = (y + y0 * d);
                /* WLH 24 April 99
                        vx[nv] = x;
                        vy[nv] = y;
                        vz[nv] = z;
                        nv++;
                        vx[nv] = x1;
                        vy[nv] = y1;
                        vz[nv] = z;
                        nv++;
                */
                // System.out.println("wsp25 " + x + " " + y + "" + x1 + " " + y1);
                // g.drawLine(x, y, x1, y1);
            }

            //now plot any 10 kt wind barbs
            barb  = 0.4f * scale;
            slant = 0.15f * scale;
            for (int j = 0; j < nbarb10; j++) {
                //d  = d + 0.25f * scale;
                d  = d + pt_size;
                x1 = (x + x0 * d);
                y1 = (y + y0 * d);
                if (south) {
                    x2 = (x + x0 * (d + slant) - y0 * barb);
                    y2 = (y + y0 * (d + slant) + x0 * barb);
                } else {
                    x2 = (x + x0 * (d + slant) + y0 * barb);
                    y2 = (y + y0 * (d + slant) - x0 * barb);
                }

                vx[nv] = x1;
                vy[nv] = y1;
                vz[nv] = z;
                nv++;
                vx[nv] = x2;
                vy[nv] = y2;
                vz[nv] = z;
                nv++;
                // System.out.println("barb10 " + j + " " + x1 + " " + y1 + "" + x2 + " " + y2);
                // g.drawLine(x1,y1,x2,y2);
            }
            /* WLH 24 April 99
                  vx[nv] = x;
                  vy[nv] = y;
                  vz[nv] = z;
                  nv++;
                  vx[nv] = x1;
                  vy[nv] = y1;
                  vz[nv] = z;
                  nv++;
            */
            // System.out.println("line " + x + " " + y + "" + x1 + " " + y1);
            // g.drawLine(x,y,x1,y1);

            //lengthen the pole to accomodate the 50 knot barbs
            if (nbarb50 > 0) {
                d = d + pt_size;
                //x1 = (x + x0 * d);
                //y1 = (y + y0 * d);
                /* WLH 24 April 99
                        vx[nv] = x;
                        vy[nv] = y;
                        vz[nv] = z;
                        nv++;
                        vx[nv] = x1;
                        vy[nv] = y1;
                        vz[nv] = z;
                        nv++;
                */
                // System.out.println("line50 " + x + " " + y + "" + x1 + " " + y1);
                // g.drawLine(x,y,x1,y1);
            }

            //plot the 50 kt wind barbs
            /* WLH 5 Nov 99
                  s195 = (float) Math.sin(195 * Data.DEGREES_TO_RADIANS);
                  c195 = (float) Math.cos(195 * Data.DEGREES_TO_RADIANS);
            */
            float flagSpace = .5f * pt_size;
            float flagWidth = 1.75f * slant;
            for (int j = 0; j < nbarb50; j++) {
                //d  = d + 0.6f * scale;
                x1 = (x + x0 * d);
                y1 = (y + y0 * d);
                x3 = (x + x0 * (d + flagWidth));
                y3 = (y + y0 * (d + flagWidth));
                /* WLH 5 Nov 99
                        if (south) {
                          x2 = (x3+barb*(x0*s195+y0*c195));
                          y2 = (y3-barb*(x0*c195-y0*s195));
                        }
                        else {
                          x2 = (x3-barb*(x0*s195+y0*c195));
                          y2 = (y3+barb*(x0*c195-y0*s195));
                        }
                */
                if (south) {
                    x2 = (x + x0 * (d + slant) - y0 * barb);
                    y2 = (y + y0 * (d + slant) + x0 * barb);
                } else {
                    x2 = (x + x0 * (d + slant) + y0 * barb);
                    y2 = (y + y0 * (d + slant) - x0 * barb);
                }
                // now lengthen the distance to the next position
                d = d + flagWidth + flagSpace;

                float[] xp = { x1, x2, x3 };
                float[] yp = { y1, y2, y3 };

                tx[nt] = x1;
                ty[nt] = y1;
                tz[nt] = z;
                nt++;
                tx[nt] = x2;
                ty[nt] = y2;
                tz[nt] = z;
                nt++;
                tx[nt] = x3;
                ty[nt] = y3;
                tz[nt] = z;
                nt++;
                /*
                System.out.println("barb50 " + x1 + " " + y1 + "" + x2 + " " + y2 +
                                 "  " + x3 + " " + y3);
                */
                // g.fillPolygon(xp,yp,3);
                //start location for the next barb
                x1 = x3;
                y1 = y3;
            }

            // WLH 24 April 99 - now plot the pole
            vx[nv] = x;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            vx[nv] = x1;
            vy[nv] = y1;
            vz[nv] = z;
            nv++;

            mbarb[2] = x1;
            mbarb[3] = y1;
        } else {  // if (wnd_spd < 2.5)

            // wind < 2.5 kts.  Plot a circle
            float rad = (0.7f * pt_size);

            // draw 8 segment circle, center = (x, y), radius = rad
            // 1st segment
            vx[nv] = x - rad;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            vx[nv] = x - 0.7f * rad;
            vy[nv] = y + 0.7f * rad;
            vz[nv] = z;
            nv++;
            // 2nd segment
            vx[nv] = x - 0.7f * rad;
            vy[nv] = y + 0.7f * rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x;
            vy[nv] = y + rad;
            vz[nv] = z;
            nv++;
            // 3rd segment
            vx[nv] = x;
            vy[nv] = y + rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x + 0.7f * rad;
            vy[nv] = y + 0.7f * rad;
            vz[nv] = z;
            nv++;
            // 4th segment
            vx[nv] = x + 0.7f * rad;
            vy[nv] = y + 0.7f * rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x + rad;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            // 5th segment
            vx[nv] = x + rad;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            vx[nv] = x + 0.7f * rad;
            vy[nv] = y - 0.7f * rad;
            vz[nv] = z;
            nv++;
            // 6th segment
            vx[nv] = x + 0.7f * rad;
            vy[nv] = y - 0.7f * rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x;
            vy[nv] = y - rad;
            vz[nv] = z;
            nv++;
            // 7th segment
            vx[nv] = x;
            vy[nv] = y - rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x - 0.7f * rad;
            vy[nv] = y - 0.7f * rad;
            vz[nv] = z;
            nv++;
            // 8th segment
            vx[nv] = x - 0.7f * rad;
            vy[nv] = y - 0.7f * rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x - rad;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            // System.out.println("circle " + x + " " + y + "" + rad);
            // g.drawOval(x-rad,y-rad,2*rad,2*rad);

            mbarb[2] = x;
            mbarb[3] = y;
        }
        //    ucar.unidata.util.Misc.printArray("mbarb end makebarb", mbarb);

        numv[0] = nv;
        numt[0] = nt;
        return mbarb;
    }

    /**
     * Adapted from Don Murray's mind
     *
     * @param south      north or south orientation flag
     * @param x          x value location
     * @param y          y value location
     * @param z          z value location
     * @param scale      scale factor
     * @param pt_size    spacing between barbs
     * @param f0         u component
     * @param f1         v component
     * @param vx         x coordinate of VisADLineArrays
     * @param vy         y coordinate of VisADLineArrays
     * @param vz         z coordinate of VisADLineArrays
     * @param numv       number of coordinates
     * @param tx         x coordinate of VisADTriangleArrays (wind flags)
     * @param ty         y coordinate of VisADTriangleArrays (wind flags)
     * @param tz         z coordinate of VisADTriangleArrays (wind flags)
     * @param numt       number of coordinates
     * @return  an array of the start and end points
     */
    public static float[] makeBarb(boolean south, float x, float y, float z,
                                   float scale, float pt_size, float f0,
                                   float f1, float[] vx, float[] vy,
                                   float[] vz, int[] numv, float[] tx,
                                   float[] ty, float[] tz, int[] numt) {
        return makeBarb(south, x, y, z, scale, pt_size, f0, f1, vx, vy, vz,
                        numv, tx, ty, tz, numt, false);

    }


    /**
     * Adapted from Don Murray's mind
     *
     * @param south      north or south orientation flag
     * @param x          x value location
     * @param y          y value location
     * @param z          z value location
     * @param scale      scale factor
     * @param pt_size    spacing between barbs
     * @param f0         u component
     * @param f1         v component
     * @param vx         x coordinate of VisADLineArrays
     * @param vy         y coordinate of VisADLineArrays
     * @param vz         z coordinate of VisADLineArrays
     * @param numv       number of coordinates
     * @param tx         x coordinate of VisADTriangleArrays (wind flags)
     * @param ty         y coordinate of VisADTriangleArrays (wind flags)
     * @param tz         z coordinate of VisADTriangleArrays (wind flags)
     * @param numt       number of coordinates
     * @param rotateToGlobe  if true, rotate vectors for globe
     * @return  an array of the start and end points
     */
    public static float[] makeBarb(boolean south, float x, float y, float z,
                                   float scale, float pt_size, float f0,
                                   float f1, float[] vx, float[] vy,
                                   float[] vz, int[] numv, float[] tx,
                                   float[] ty, float[] tz, int[] numt,
                                   boolean rotateToGlobe) {

        float   wsp25, slant, barb, d, c195, s195;
        float   x0, y0;
        float   x1, y1, x2, y2, x3, y3;
        int     nbarb50, nbarb10, nbarb5;

        float[] mbarb = new float[4];
        mbarb[0] = x;
        mbarb[1] = y;
        //    ucar.unidata.util.Misc.printArray("mbarb begin makebarb", mbarb);

        // convert meters per second to knots
        f0 *= (3600.0 / 1853.248);
        f1 *= (3600.0 / 1853.248);

        float wnd_spd = (float) Math.sqrt(f0 * f0 + f1 * f1);
        //System.out.println("Speed = " + wnd_spd);
        int lenv    = vx.length;
        int lent    = tx.length;
        int nv      = numv[0];
        int nt      = numt[0];
        int maxbas  = 6;
        int nvStart = nv;
        int ntStart = nt;

        //determine the initial (minimum) length of the flag pole
        if (wnd_spd > 0) {

            wsp25 = (float) Math.max(wnd_spd + 2.5, 5.0);
            slant = 0.15f * scale;  // amount of slant
            barb  = (wnd_spd <= 2.5) ? 0: 0.5f * scale;   // length of a barb;
            // WLH 6 Aug 99 - barbs point the other way (duh)
            x0 = -f0 / wnd_spd;
            y0 = -f1 / wnd_spd;

            //determine number of wind barbs needed for 10 and 50 kt winds
            nbarb50 = (int) (wsp25 / 50.f);
            nbarb10 = (int) ((wsp25 - (nbarb50 * 50.f)) / 10.f);
            nbarb5 = (int) ((wsp25 - (nbarb50 * 50.f) - (nbarb10 * 10.f))
                            / 5.f);
            int nbas = nbarb50 * 2 + nbarb10 + nbarb5;
            nbas = Math.max(nbas, maxbas);
            //nbas = 6;
            float barbSpace = .7f * pt_size;

            //length of the flag pole
            d        = nbas * pt_size;
            x1       = (x + x0 * d);
            y1       = (y + y0 * d);
            mbarb[2] = x1;
            mbarb[3] = y1;

            float flagSpace = .3333f * barbSpace;
            float flagWidth = 1.6666f * barbSpace;
            float flagSlant = flagWidth - slant;
            for (int j = 0; j < nbarb50; j++) {
                x1 = (x + x0 * d);
                y1 = (y + y0 * d);
                x3 = (x + x0 * (d - flagWidth));
                y3 = (y + y0 * (d - flagWidth));
                if (south) {
                    x2 = (x + x0 * (d - flagSlant) - y0 * barb);
                    y2 = (y + y0 * (d - flagSlant) + x0 * barb);
                } else {
                    x2 = (x + x0 * (d - flagSlant) + y0 * barb);
                    y2 = (y + y0 * (d - flagSlant) - x0 * barb);
                }
                // now lengthen the distance to the next position
                d      = d - flagWidth - flagSpace;

                tx[nt] = x1;
                ty[nt] = y1;
                tz[nt] = z;
                nt++;
                tx[nt] = x2;
                ty[nt] = y2;
                tz[nt] = z;
                nt++;
                tx[nt] = x3;
                ty[nt] = y3;
                tz[nt] = z;
                nt++;
                /*
                System.out.println("barb50 " + x1 + " " + y1 + "" + x2 + " " + y2 +
                                 "  " + x3 + " " + y3);
                */
                //start location for the next barb
                x1 = x3;
                y1 = y3;
            }
            // add back a little
            if (nbarb50 > 0) {
                d = d - .5f * barbSpace;
            }

            //now plot any 10 kt wind barbs
            for (int j = 0; j < nbarb10; j++) {
                x1 = (x + x0 * d);
                y1 = (y + y0 * d);
                if (south) {
                    x2 = (x + x0 * (d + slant) - y0 * barb);
                    y2 = (y + y0 * (d + slant) + x0 * barb);
                } else {
                    x2 = (x + x0 * (d + slant) + y0 * barb);
                    y2 = (y + y0 * (d + slant) - x0 * barb);
                }
                d      = d - barbSpace;

                vx[nv] = x1;
                vy[nv] = y1;
                vz[nv] = z;
                nv++;
                vx[nv] = x2;
                vy[nv] = y2;
                vz[nv] = z;
                nv++;
                // System.out.println("barb10 " + j + " " + x1 + " " + y1 + "" + x2 + " " + y2);
            }

            //2.5 to 7.5 kt winds are plotted with the barb part way done the pole
            if ((nbarb50 == 0) && (nbarb10 == 0)) {
                d = d - barbSpace;
            }
            if (nbarb5 == 1) {
                barb  = barb * 0.6f;
                slant = slant * 0.6f;
                x1    = (x + x0 * d);
                y1    = (y + y0 * d);

                if (south) {
                    x2 = (x + x0 * (d + slant) - y0 * barb);
                    y2 = (y + y0 * (d + slant) + x0 * barb);
                } else {
                    x2 = (x + x0 * (d + slant) + y0 * barb);
                    y2 = (y + y0 * (d + slant) - x0 * barb);
                }

                vx[nv] = x1;
                vy[nv] = y1;
                vz[nv] = z;
                nv++;
                vx[nv] = x2;
                vy[nv] = y2;
                vz[nv] = z;
                nv++;
                // System.out.println("barb5 " + x1 + " " + y1 + "" + x2 + " " + y2);
            }


            // System.out.println("line " + x + " " + y + "" + x1 + " " + y1);

            // WLH 24 April 99 - now plot the pole
            vx[nv] = x;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            vx[nv] = mbarb[2];
            vy[nv] = mbarb[3];
            vz[nv] = z;
            nv++;


        } else {  // if (wnd_spd < 2.5)

            // wind < 2.5 kts.  Plot a circle
            float rad = (0.7f * pt_size);

            // draw 8 segment circle, center = (x, y), radius = rad
            // 1st segment
            vx[nv] = x - rad;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            vx[nv] = x - 0.7f * rad;
            vy[nv] = y + 0.7f * rad;
            vz[nv] = z;
            nv++;
            // 2nd segment
            vx[nv] = x - 0.7f * rad;
            vy[nv] = y + 0.7f * rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x;
            vy[nv] = y + rad;
            vz[nv] = z;
            nv++;
            // 3rd segment
            vx[nv] = x;
            vy[nv] = y + rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x + 0.7f * rad;
            vy[nv] = y + 0.7f * rad;
            vz[nv] = z;
            nv++;
            // 4th segment
            vx[nv] = x + 0.7f * rad;
            vy[nv] = y + 0.7f * rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x + rad;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            // 5th segment
            vx[nv] = x + rad;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            vx[nv] = x + 0.7f * rad;
            vy[nv] = y - 0.7f * rad;
            vz[nv] = z;
            nv++;
            // 6th segment
            vx[nv] = x + 0.7f * rad;
            vy[nv] = y - 0.7f * rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x;
            vy[nv] = y - rad;
            vz[nv] = z;
            nv++;
            // 7th segment
            vx[nv] = x;
            vy[nv] = y - rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x - 0.7f * rad;
            vy[nv] = y - 0.7f * rad;
            vz[nv] = z;
            nv++;
            // 8th segment
            vx[nv] = x - 0.7f * rad;
            vy[nv] = y - 0.7f * rad;
            vz[nv] = z;
            nv++;
            vx[nv] = x - rad;
            vy[nv] = y;
            vz[nv] = z;
            nv++;
            // System.out.println("circle " + x + " " + y + "" + rad);
            // g.drawOval(x-rad,y-rad,2*rad,2*rad);

            mbarb[2] = x;
            mbarb[3] = y;
        }
        //    ucar.unidata.util.Misc.printArray("mbarb end makebarb", mbarb);
        if (rotateToGlobe && false) {
            float[] oneBarbVx = new float[nv - nvStart];
            float[] oneBarbVy = new float[nv - nvStart];
            float[] oneBarbVz = new float[nv - nvStart];
            float[] oneBarbTx = new float[nt - ntStart];
            float[] oneBarbTy = new float[nt - ntStart];
            float[] oneBarbTz = new float[nt - ntStart];

            System.arraycopy(vx, nvStart, oneBarbVx, 0, oneBarbVx.length);
            System.arraycopy(vy, nvStart, oneBarbVy, 0, oneBarbVy.length);
            System.arraycopy(vz, nvStart, oneBarbVz, 0, oneBarbVz.length);
            System.arraycopy(tx, ntStart, oneBarbTx, 0, oneBarbTx.length);
            System.arraycopy(ty, ntStart, oneBarbTy, 0, oneBarbTy.length);
            System.arraycopy(tz, ntStart, oneBarbTz, 0, oneBarbTz.length);

            Transform3D t3d       = new Transform3D();
            float[][]   latlonrad = null;

            t3d.set(new Vector3f(new float[] { -x, -y, -z }));
            applyTransform(t3d, oneBarbVx, oneBarbVy, oneBarbVz);
            applyTransform(t3d, oneBarbTx, oneBarbTy, oneBarbTz);

            try {
                CoordinateSystem dspCS = Display.DisplaySphericalCoordSys;
                latlonrad = dspCS.fromReference(new float[][] {
                    { x }, { y }, { z }
                });
            } catch (Exception e) {
                System.out.println(e);
            }

            float latLoc = latlonrad[0][0];
            float lonLoc = latlonrad[1][0];
            latLoc *= (float) Data.DEGREES_TO_RADIANS;
            lonLoc *= (float) Data.DEGREES_TO_RADIANS;

            t3d.rotZ(-lonLoc);
            applyTransform(t3d, oneBarbVx, oneBarbVy, oneBarbVz);
            applyTransform(t3d, oneBarbTx, oneBarbTy, oneBarbTz);

            if (latLoc >= 0) {
                t3d.rotY(90 * Data.DEGREES_TO_RADIANS - Math.abs(latLoc));
            } else {
                t3d.rotY(-(90 * Data.DEGREES_TO_RADIANS - Math.abs(latLoc)));
            }
            applyTransform(t3d, oneBarbVx, oneBarbVy, oneBarbVz);
            applyTransform(t3d, oneBarbTx, oneBarbTy, oneBarbTz);

            t3d.rotZ(lonLoc);
            applyTransform(t3d, oneBarbVx, oneBarbVy, oneBarbVz);
            applyTransform(t3d, oneBarbTx, oneBarbTy, oneBarbTz);

            t3d.set(new Vector3f(new float[] { x, y, z }));
            applyTransform(t3d, oneBarbVx, oneBarbVy, oneBarbVz);
            applyTransform(t3d, oneBarbTx, oneBarbTy, oneBarbTz);

            System.arraycopy(oneBarbVx, 0, vx, nvStart, oneBarbVx.length);
            System.arraycopy(oneBarbVy, 0, vy, nvStart, oneBarbVy.length);
            System.arraycopy(oneBarbVz, 0, vz, nvStart, oneBarbVz.length);

            System.arraycopy(oneBarbTx, 0, tx, ntStart, oneBarbTx.length);
            System.arraycopy(oneBarbTy, 0, ty, ntStart, oneBarbTy.length);
            System.arraycopy(oneBarbTz, 0, tz, ntStart, oneBarbTz.length);
        }

        numv[0] = nv;
        numt[0] = nt;
        return mbarb;
    }

    /**
     * Apply the transform to the points
     *
     * @param t3d  the transform
     * @param xPts  xpoints
     * @param yPts  ypoints
     * @param zPts  zpoints
     */
    private static void applyTransform(Transform3D t3d, float[] xPts,
                                      float[] yPts, float[] zPts) {
        Point3f pt3f = new Point3f();
        float[] tmp  = new float[3];
        for (int k = 0; k < xPts.length; k++) {
            pt3f.set(xPts[k], yPts[k], zPts[k]);
            t3d.transform(pt3f);
            pt3f.get(tmp);
            xPts[k] = tmp[0];
            yPts[k] = tmp[1];
            zPts[k] = tmp[2];
        }
    }

}
