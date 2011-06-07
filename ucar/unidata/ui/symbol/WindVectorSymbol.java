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

import java.awt.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Jun 6, 2011
 * Time: 12:05:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class WindVectorSymbol extends MetSymbol {

    /** Fix conversion value */
    private static final double DEG_TO_RAD = (Math.PI / 180.0);

    /** The example windspeed */
    private double windSpeed;  // knots

    /** The example wind dir */
    private double windDirection;  // degrees; clockwise 0 = North

    /** for drawing */
    private int lenBarb;  // length of the Barb

    /** for drawing */
    private double sint, cost;


    /** my drawer */
    private Drawer drawer;


    /**
     * Default constructor.
     */
    public WindVectorSymbol() {
        //setRectPoint(PT_MM);
    }

    /**
     * Construct a WindBarbSymbol to use on the canvas specified at the
     * position specified.  Use the default names and long names.
     * @param canvas  canvas to draw on.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     */
    public WindVectorSymbol(DisplayCanvas canvas, int x, int y) {
        this(canvas, x, y, "U", "U or windspeed parameter", "V",
             "V or direction parameter");
    }

    /**
     * Construct a WindBarbSymbol without a canvas at the
     * position specified.  Use the parameter names and long names specified.
     * @param x              x position
     * @param y              y position
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
     * Construct a WindBarbSymbol to use on the canvas specified at the
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
        super(canvas, x, y, new String[] { uOrSpeedParam, vOrDirParam },
              new String[] { uOrSpeedDescr,
                             vOrDirDescr });
        //rectPoint = PT_MM;
        setSize(20, 20);
        setWindDirection(300.0);
        setWindSpeed(65.0);
    }



    /**
     * Should this shape be scaled. See MetSymbol.
     *
     * @return Should this shape be scaled
     */
    public boolean shouldScaleShape() {
        return false;
    }

    /**
     * Should this shape be offset. See MetSymbol.
     *
     * @return Should this shape be offset
     */
    public boolean shouldOffsetShape() {
        return false;
    }

    /**
     * Determine whether this <code>MetSymbol</code> should show an
     * alignment menu in an editor.  Subclasses should override if not.
     * @return false for this
     */
    public boolean doAlignmentMenu() {
        return false;
    }

    /**
     * Get the parameter value at the index specified.
     * @param  index  index into param array
     * @return value of uOrSpeedParam if index = 0, otherwise vOrDirParam value
     */
    public Object getParamValue(int index) {
        return ((index == 0)
                ? new Double(windSpeed)
                : new Double(windDirection));
    }

    /**
     * Set the parameter value at the index specified.
     * @param  index  index into param array
     * @param  v      value (<code>String</code>) of double parameter value
     */
    public void setParamValue(int index, Object v) {
        if (index == 0) {
            setWindSpeed(new Double(v.toString()).doubleValue());
        } else {
            setWindDirection(new Double(v.toString()).doubleValue());
        }
    }

    /**
     * Get the name of the Speed or U component.
     * @return name of this component.
     */
    public String getSpeedName() {
        return getParam(0);
    }

    /**
     * Get the name of the Direction or V component.
     * @return name of this component.
     */
    public String getDirectionName() {
        return getParam(1);
    }

    /**
     * Get whether this <code>MetSymbol</code> can be stretched or not.
     * @return true if can be stretched.
     */
    public boolean getStretchy() {
        return !getBeingCreated();
    }

    /**
     * Get whether this <code>MetSymbol</code> has equals sides
     * (width and height).
     * @return  true
     */
    public boolean getEqualSides() {
        return true;
    }

    /**
     * Get whether this <code>MetSymbol</code> should be stretched
     * symetrically or not.
     * @return true
     */
    public boolean getSymetricReshape() {
        return true;
    }

    /**
     * get the Wind Speed, in knots
     * @return windSpeed
     */
    public double getWindSpeed() {
        return windSpeed;
    }

    /**
     * set the Wind Speed, in knots
     * @param  windSpeed speed in knots
     */
    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    /**
     * get the Wind Direction, in degrees (0 = north) from [0, 360)
     * @return The direction
     */
    public double getWindDirection() {
        return windDirection;
    }

    /**
     * set the Wind Direction, in degrees (0 = north) from [0, 360)
     *
     * @param windDirection
     */
    public void setWindDirection(double windDirection) {
        this.windDirection =
            ucar.unidata.geoloc.LatLonPointImpl.lonNormal360(windDirection);
    }




    /**
     *     draw the symbol at the specified location
     *     @param  g        Graphics to draw to
     *     @param  x        x location
     *     @param  y        y location
     *     @param  width    width to draw
     *     @param  height   height to draw
     */
    public void draw(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(getForeground());
        if (drawer == null) {
            drawer = new Drawer();
        }
        drawer.draw(g, x, y, width, height, windSpeed, windDirection);
    }



    /**
     * Class Drawer knows how to draw windbarbs
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.23 $
     */
    public static class Drawer {

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
        public Drawer() {}

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

            // calc how long the windBarb should be
            int len = 0;
            while (speed >= 47.5) {
                len   += 6;
                speed -= 50;
            }
            while (speed >= 7.5) {
                len   += 3;
                speed -= 10;
            }
            if (speed >= 2.5) {
                len += 3;
            }
            lenBarb = Math.max(len, width);

            drawRotatedLine(g, 0, 0, 0, -lenBarb);
            sint = Math.sin(theta + 15.0 * DEG_TO_RAD);
            cost = Math.cos(theta + 15.0 * DEG_TO_RAD);
            drawRotatedLine(g, 0, 0, 0, -lenBarb / 2);
            sint = Math.sin(theta - 15.0 * DEG_TO_RAD);
            cost = Math.cos(theta - 15.0 * DEG_TO_RAD);
            drawRotatedLine(g, 0, 0, 0, -lenBarb / 2);
        }

        /**
         * draw line
         *
         * @param g graphics
         * @param x1 _more_
         * @param y1 _more_
         * @param x2 _more_
         * @param y2 _more_
         *
         */
        private void drawRotatedLine(Graphics2D g, double x1, double y1,
                                     double x2, double y2) {
            int begx = (int) (x1 * cost - y1 * sint);
            int begy = (int) (x1 * sint + y1 * cost);

            int endx = (int) (x2 * cost - y2 * sint);
            int endy = (int) (x2 * sint + y2 * cost);

            g.drawLine(x0 + begx, y0 + begy, x0 + endx, y0 + endy);
        }

    }

    /**
     * Can we rotate this symbol when the display rotates
     *
     * @return _more_
     */
    public boolean rotateOnEarth() {
        return false;
    }



}
