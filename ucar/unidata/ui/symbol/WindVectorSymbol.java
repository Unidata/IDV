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
package ucar.unidata.ui.symbol;


import ucar.unidata.ui.drawing.DisplayCanvas;

import visad.VisADException;
import visad.VisADGeometryArray;
import visad.VisADLineArray;

import java.awt.Graphics2D;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Wed, Jun 8, '11
 * @author         Enter your name here...    
 */
public class WindVectorSymbol extends WindBarbSymbol {

    /**
     * Create a WindVectorSymbol
     */
    public WindVectorSymbol() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Create a wind vector symbol
     *
     * @param canvas  the canvas
     * @param x  the x position
     * @param y  the y position
     */
    public WindVectorSymbol(DisplayCanvas canvas, int x, int y) {
        this(canvas, x, y, "U", "U or speed parameter", "V",
             "V or direction parameter");
    }

    /**
     * Construct a WindVectorSymbol to use on the canvas specified at the
     * position specified.  Use the parameter names and long names specified.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     * @param uOrSpeedParam  u or speed component of wind parameter name
     * @param uOrSpeedDescr  u or speed component of wind parameter description
     * @param vOrDirParam    v or direction component of wind parameter name
     * @param vOrDirDescr    v or direction component of wind parameter descr
     */
    public WindVectorSymbol(int x, int y, String uOrSpeedParam,
                            String uOrSpeedDescr, String vOrDirParam,
                            String vOrDirDescr) {
        this(null, x, y, uOrSpeedParam, uOrSpeedDescr, vOrDirParam,
             vOrDirDescr);
    }

    /**
     * Construct a WindVectorSymbol to use on the canvas specified at the
     * position specified.  Use the parameter names and long names specified.
     * @param canvas  canvas to draw on.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     * @param uOrSpeedParam  u or speed component of wind parameter name
     * @param uOrSpeedDescr  u or speed component of wind parameter description
     * @param vOrDirParam    v or direction component of wind parameter name
     * @param vOrDirDescr    v or direction component of wind parameter descr
     */
    public WindVectorSymbol(DisplayCanvas canvas, int x, int y,
                            String uOrSpeedParam, String uOrSpeedDescr,
                            String vOrDirParam, String vOrDirDescr) {
        super(canvas, x, y, uOrSpeedParam, uOrSpeedDescr, vOrDirParam,
              vOrDirDescr);
    }

    /**
     * Make the drawer for this symbol
     * @return the drawer
     */
    protected WindDrawer makeDrawer() {
        return new VectorDrawer();
    }

    /** back scaling */
    private static final float BACK_SCALE = -0.15f;

    /** forward scaling */
    private static final float PERP_SCALE = 0.15f;

    /**
     * make the vector.  Adapted from visad.ShadowType.makeFlow.
     *
     * @param flow_values  the flow values (u,v)
     * @param flowScale  the scale
     * @param spatial_values  the spatial locations (x,y)
     * @param color_values  color (not handled here)
     * @param range_select  missing flags
     *
     * @return the drawn vector
     *
     * @throws VisADException  problem drawing the vector
     */
    public VisADGeometryArray[] makeVector(float[][] flow_values,
                                           float flowScale,
                                           float[][] spatial_values,
                                           byte[][] color_values,
                                           boolean[][] range_select)
            throws VisADException {

        if (flow_values[0] == null) {
            return null;
        }
        if (spatial_values[0] == null) {
            return null;
        }

        VisADLineArray array = new VisADLineArray();

        int            len   = spatial_values[0].length;
        int            flen  = flow_values[0].length;
        int            rlen  = 0;  // number of non-missing values
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

        array.vertexCount = 6 * rlen;

        float[] coordinates = new float[18 * rlen];
        int     m           = 0;
        // flow vector
        float f0 = 0.0f,
              f1 = 0.0f,
              f2 = 0.0f;
        // arrow head vector
        float a0 = 0.0f,
              a1 = 0.0f,
              a2 = 0.0f;
        float b0 = 0.0f,
              b1 = 0.0f,
              b2 = 0.0f;
        for (int j = 0; j < len; j++) {
            if ((range_select[0] == null) || range_select[0][j]) {
                if (flen == 1) {
                    f0 = flowScale * flow_values[0][0];
                    f1 = flowScale * flow_values[1][0];
                    f2 = flowScale * flow_values[2][0];
                } else {
                    f0 = flowScale * flow_values[0][j];
                    f1 = flowScale * flow_values[1][j];
                    f2 = flowScale * flow_values[2][j];
                }
                int k = m;
                // base point of flow vector
                coordinates[m++] = spatial_values[0][j];
                coordinates[m++] = spatial_values[1][j];
                coordinates[m++] = spatial_values[2][j];
                int n = m;
                // k = orig m
                // m = orig m + 3
                // end point of flow vector
                coordinates[m++] = coordinates[k++] + f0;
                coordinates[m++] = coordinates[k++] + f1;
                coordinates[m++] = coordinates[k++] + f2;
                k                = n;
                // n = orig m + 3
                // m = orig m + 6
                // repeat end point of flow vector as
                // first point of first arrow head
                coordinates[m++] = coordinates[n++];
                coordinates[m++] = coordinates[n++];
                coordinates[m++] = coordinates[n++];
                b0               = a0 = BACK_SCALE * f0;
                b1               = a1 = BACK_SCALE * f1;
                b2               = a2 = BACK_SCALE * f2;

                if (  /*mode2d
                      || */
                ((Math.abs(f2) <= Math.abs(f0))
                        && (Math.abs(f2) <= Math.abs(f1)))) {
                    a0 += PERP_SCALE * f1;
                    a1 -= PERP_SCALE * f0;
                    b0 -= PERP_SCALE * f1;
                    b1 += PERP_SCALE * f0;
                }  /*else if (Math.abs(f1) <= Math.abs(f0)) {
                   a0 += PERP_SCALE * f2;
                   a2 -= PERP_SCALE * f0;
                   b0 -= PERP_SCALE * f2;
                   b2 += PERP_SCALE * f0;
                 } else { // f0 is least
                   a1 += PERP_SCALE * f2;
                   a2 -= PERP_SCALE * f1;
                   b1 -= PERP_SCALE * f2;
                   b2 += PERP_SCALE * f1;
                 }
                 */

                k = n;
                // n = orig m + 6
                // m = orig m + 9
                // second point of first arrow head
                coordinates[m++] = coordinates[n++] + a0;
                coordinates[m++] = coordinates[n++] + a1;
                coordinates[m++] = coordinates[n++] + a2;

                n                = k;
                // k = orig m + 6
                // first point of second arrow head
                coordinates[m++] = coordinates[k++];
                coordinates[m++] = coordinates[k++];
                coordinates[m++] = coordinates[k++];

                // n = orig m + 6
                // second point of second arrow head
                coordinates[m++] = coordinates[n++] + b0;
                coordinates[m++] = coordinates[n++] + b1;
                coordinates[m++] = coordinates[n++] + b2;
            }
        }
        array.coordinates = coordinates;
        return new VisADGeometryArray[] { array };

    }

    /**
     * Class VectorDrawer knows how to draw windbarbs
     *
     * @author IDV Development Team
     */
    public static class VectorDrawer extends WindDrawer {

        /** attrs */
        int x0, y0;

        /** attrs */
        double lenBarb;

        /** attrs */
        double sint;

        /** attrs */
        double cost;

        /** attrs */
        double windSpeed;

        /**
         * ctor
         */
        public VectorDrawer() {}

        /**
         * draw
         *
         * @param g graphics
         * @param x x
         * @param y y
         * @param width width
         * @param height height
         * @param speed wind speed
         * @param dirDegrees wind dir
         */
        public void draw(Graphics2D g, int x, int y, int width, int height,
                         double speed, double dirDegrees) {

            this.windSpeed = speed;
            this.x0        = x0;
            this.y0        = y0;
            double theta = dirDegrees * DEG_TO_RAD;
            sint = Math.sin(theta);
            cost = Math.cos(theta);

            x0   = x + width / 2;
            y0   = y + height / 2;

            // calc how long the wind vector should be
            lenBarb = width;

            drawRotatedLine(g, 0, 0, 0, lenBarb);
            drawRotatedLine(g, 0, lenBarb, lenBarb / 4, 3 * lenBarb / 4);
            drawRotatedLine(g, 0, lenBarb, -lenBarb / 4, 3 * lenBarb / 4);

        }


        /**
         * Draw a line
         *
         * @param g graphics
         * @param start start degrees
         * @return next degrees
         */
        private int draw10knotLine(Graphics2D g, int start) {
            drawRotatedLine(g, 0, -lenBarb + start, .4 * lenBarb,
                            -lenBarb + start - 3);
            return start + 3;
        }

        /**
         * Draw the flag
         *
         * @param g graphics
         * @param start degrees
         * @return next degrees
         */
        private int draw50knotFlag(Graphics2D g, int start) {
            drawRotatedTriangle(g, 0, -lenBarb + start + 4, .4 * lenBarb + 2,
                                -lenBarb + start - 1, 0,
                                -lenBarb + start - 1);
            return start + 6;
        }

        /**
         * draw line
         *
         * @param g graphics
         * @param x1 x1
         * @param y1 y1
         * @param x2 x2
         * @param y2 y2
         */
        private void drawRotatedLine(Graphics2D g, double x1, double y1,
                                     double x2, double y2) {
            int begx = (int) (x1 * cost - y1 * sint);
            int begy = (int) (x1 * sint + y1 * cost);

            int endx = (int) (x2 * cost - y2 * sint);
            int endy = (int) (x2 * sint + y2 * cost);

            g.drawLine(x0 + begx, y0 + begy, x0 + endx, y0 + endy);
        }

        /**
         * draw triangle
         *
         * @param g graphics
         * @param x1 x1
         * @param y1 y1
         * @param x2 x2
         * @param y2 y2
         * @param x3 x3
         * @param y3 y3
         */
        private void drawRotatedTriangle(Graphics2D g, double x1, double y1,
                                         double x2, double y2, double x3,
                                         double y3) {

            /** for drawing */
            int[] xPoint = new int[3];

            /** for drawing */
            int[] yPoint = new int[3];

            xPoint[0] = x0 + (int) (x1 * cost - y1 * sint);
            yPoint[0] = y0 + (int) (x1 * sint + y1 * cost);

            xPoint[1] = x0 + (int) (x2 * cost - y2 * sint);
            yPoint[1] = y0 + (int) (x2 * sint + y2 * cost);

            xPoint[2] = x0 + (int) (x3 * cost - y3 * sint);
            yPoint[2] = y0 + (int) (x3 * sint + y3 * cost);

            g.fillPolygon(xPoint, yPoint, 3);
        }

    }
}
