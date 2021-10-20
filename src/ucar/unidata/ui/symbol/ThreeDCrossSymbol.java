/*
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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

import ucar.unidata.data.point.PointOb;
import ucar.unidata.ui.drawing.DisplayCanvas;
import ucar.visad.ShapeUtility;
import visad.*;

import java.awt.*;

/**
 * draws a symbol for a location point indicaiton,
 * in 3D which is very simple (small number of graphic elements)
 * so that large numbers of symbols can be displayed (like 5000).
 * @author
 * @version $Id:  $
 */
public class ThreeDCrossSymbol extends MetSymbol {

    // gps vector variables
    //private double ve=0.0, vn=0.0, sigx=0.0, sigy=0.0, corrxy=0.0;

    // initial line of 3 is at angle 0 degrees
    //private double sint=0.0, cost=1.0;

    /** Can this shape resize */
    boolean stretchy = true;

    /** The weather symbol shapes */
    VisADGeometryArray[] shapes = null;
    int x0;
    int y0;

    /**
     * Default constructor
     */
    public ThreeDCrossSymbol() {}


    /**
     * Create a symbol at the x and y position of the "station model editor"
     * ; this is relative to the editor's screen center point.
     * @param  x   x offset from center
     * @param  y   y offset from center
     */
    public ThreeDCrossSymbol(int x, int y) {
        this(null, x, y);
    }

    /**
     * Create a symbol at the x and y position on the
     * supplied canvas.  Position is relative to the center point.
     * Use default parameter and description.
     * @param  canvas   <code>DisplayCanvas</code> to draw on
     * @param  x        x offset from center
     * @param  y        y offset from center
     */
    public ThreeDCrossSymbol(DisplayCanvas canvas, int x, int y) {
        this(canvas, x, y, "CC", "3d Cross Symbol");
        setSize(20, 20); // what for?
    }

    /**
     * Create a symbol at the x and y position.  Position
     * is relative to the center point.  Use parameter and description
     * provided.
     * @param  param  parameter name for this <code> GPSVectorSymbol</code>
     * @param  paramDesc  description (long name) of <code>param</code>
     * @param  x   x offset from center
     * @param  y   y offset from center
     */
    public ThreeDCrossSymbol(int x, int y, String param, String paramDesc) {
        this(null, x, y, param, paramDesc);
    }

    /**
     * Create a symbol at the x and y position on the
     * <code>DisplayCanvas</code> supplied.  Position
     * is relative to the center point.  Use parameter and description
     * provided.
     * @param  canvas   <code>DisplayCanvas</code> to draw on
     * @param  param  parameter name for this <code> GPSVectorSymbol</code>
     * @param  paramDesc  description (long name) of <code>param</code>
     * @param  x   x offset from center
     * @param  y   y offset from center
     */
    public ThreeDCrossSymbol(DisplayCanvas canvas, int x, int y,
                             String param, String paramDesc) {
        super(canvas, x, y, new String[]{ param }, new String[]{ paramDesc });
        //setSize(20, 20); // not needed.  Does it reset size when display is zoomed?
    }

    /**
     From Unidata: "I have code that takes the shapes
     for a symbol and moves them by some offset wrt the origin.
     However, I move each individual shape in the array. Thus the recentering all problem.
     A workaround for now is to overwrite the method shouldOffsetShape().
     This turns off any relative repositioning."
     */
    public boolean shouldOffsetShape() {
        return false;
    }

    /**
     * Add any components to the list of widgets for the main property dialog
     * tab.
     *
     * @param comps List of components.
     */
    protected void initPropertyComponents(java.util.List comps) {
        super.initPropertyComponents(comps);
    }

    /**
     * Apply properties from the properties dialog.
     */
    protected void applyProperties() {
        super.applyProperties();
    }

    /**
     * Get whether this <code>MetSymbol</code> can be stretched or not.
     * @return true if can be stretched.
     */
    public boolean getStretchy() {
        return stretchy;
    }

    /**
     * Is this stretchy
     * @param s Is this stretchy
     */
    public void setStretchy(boolean s) {
        stretchy = s;
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
     * Get the parameter value at the index specified. ??? fix
     * @param  index  index into param array
     * @return always returns the magnitude as a <code>Double</code>
     */
    public Object getParamValue(int index) {
        return new Double(0);
    }

    /**
     * Set the parameter value at the index specified.  Since this
     * <code>MetSymbol</code> only has one parameter, it sets the
     * value at index 0.
     * @param  index  parameter index.  Ignored in this object.
     * @param  v      value for the parameter (String representation of
     *                magnitude).
     * @see
     */
    public void setParamValue(int index, Object v) {
        ;// setVe(new Double(v.toString()).doubleValue());
    }

    /**
     * Should this symbol do all of the observations. Used by the
     * StationModelDisplayable to determine  if makeShapes should be called.
     *
     * @return Do all obs - true.
     */
    public boolean doAllObs() {
        return true;
    }

    /**
     * Is used by the StationModelDisplayable to determine if the color
     * of the shapes should be set.
     NOTE - if true, that means the IDV core code OVERRIDES colors defined here;
     * and everything is drawn pink or black or something, all the same color.
     *
     * @return Should the shapes be colored
     */
    public boolean shouldBeColored() {
        return false;
    }

    /**
     * Make the "shapes", the plot symbol.
     *  one symbol is made of several "shapes"; for one data point observation
     *  In this case a 3D "cross" of 3 straight arms.
     * @param ob The observation, or data value in a PointOb object.
     * @return The array of shapes.
     */
    public VisADGeometryArray[] makeShapes(PointOb ob)
    {
        Tuple  data  = (Tuple) ob.getData();
        // parm's observed data value or array of values is in "data"
        Real[] reals = null;
        try {
            reals = data.getRealComponents();
        }
        catch (Exception t) {
            System.out.println("    ThreeDCrossSymbol: makeShapes() : the PointOb  "
                    +ob+" failed in getRealComponents");
        }

        TupleType tType  = (TupleType) data.getType();

        if (reals == null) {
            System.out.println(
                    " one point obs' data is missing. The PointOb data object is "+ob.toString());
            return null;
        }

        String source=null;

        for (int k=0; k<reals.length; k++) {
            try {
                source = (tType.getComponent(k)).toString() ;
            } catch (Exception t) {
                System.out.println
                        (" failed in ThreeDCross symbols source= (tType.getComponent(k)).toString()" );
            }

           /*
           if (source.indexOf("ve") >= 0) {
               ve_Index=k;
               ve =  (reals[ve_Index]).getValue();
               }
           if (source.indexOf("vn") >= 0) {
               vn_Index=k;
               vn =  (reals[vn_Index]).getValue();
               }
           if (source.indexOf("a") >= 0) {
               ea_Index=k;
               sigx =  (reals[ea_Index]).getValue();
               }
           if (source.indexOf("b") >= 0) {
               eb_Index=k;
               sigy =  (reals[eb_Index]).getValue();
               }
           if (source.indexOf("corr") >= 0) {
               corr_Index=k;
               corrxy =  (reals[corr_Index]).getValue();
               }
           }

           double parm1, parm2;

           parm1 = ve; //vel east vector comp in mm/yr
           parm2 = vn; //vel north  vector componenet,

            float xs=0.0f, ys=0.0f, dx=0.0f, dy=0.0f;
            double dtr = Math.PI / 180.0;
            double pio2 = Math.PI/2;

            // vector azimuth in cartesian xyz system; arctan of vn/ve velocity componenets
            double STRIKE = Math.atan2(parm2, parm1);
            // strike is in Cartesian convention - CCW from due east, which is (90 - eps2 azimuth CW from north)
            double saz = STRIKE; // EPS2 axis  azimuth, converted to radians

            float d = 0.15f; //  CONTROLS WIDTH OF vector shaft;  arrow half width; full VisAD wireframe box is 1.00 wide
            */


            VisADGeometryArray shape7=null, shape3=null;

            float xs=0.0f, ys=0.0f, zs=0.0f; // center of cross at exact data location; no offset
            float dx=1.0f, dy=1.0f, dz=1.0f; // length of cross arms; 1=full visad box?
            float sizer = 0.5f; // to adjust size of plot to less than visad box width?
            dx *= sizer;
            dy *= sizer;
            dz *= sizer;

            shape7            = new VisADLineArray();
            shape7.coordinates = new float[] {
                    xs+dx, ys, zs,

                    xs-dx, ys, zs,//2
                    xs-dx, ys, zs,

                    xs,ys,zs,//2b
                    xs,ys,zs,//2b

                    xs, ys+dy, zs,//3
                    xs, ys+dy, zs,

                    xs, ys-dy, zs, //4
                    xs, ys-dy, zs ,

                    xs,ys,zs,//4b
                    xs,ys,zs,//4b

                    xs, ys, zs+dz, //5
                    xs, ys, zs+dz,

                    xs, ys, zs-dz //6
            };

            shape7.vertexCount = shape7.coordinates.length / 3;
            ShapeUtility.setColor(shape7, (new  Color(65,105,205)) );// royalblue
            shapes = new VisADGeometryArray[]{ shape7 };
        }
        return shapes;
    }

    /**
     * draw a little cross
     * at the specified location in the station model editor only -- NOT the IDV data display.
     * NOTE THIS ONLY DRAWS IN THE STATION MODEL EDITOR WINDOW; IT DOES NOT MAKE THE
     * DATA DISPLAY SYMBOLS;
     * @param g Graphics2D object
     * @param x x position
     * @param y y position
     * @param width width of symbol
     * @param height height of symbol
     */
    public void draw(Graphics2D g, int x, int y, int width, int height) {
        // to position  symbol in the "station model editor" window
        int cx = x ;//+ width / 2;
        int cy = y ;//+ height / 2;
        x0 = cx;
        y0 = cy;
        int begx = -15;
        int begy = 0;
        int endx = 15;
        int endy = 0;
        g.drawLine(x0 + begx, y0 + begy, x0 + endx, y0 + endy);
        begx = 0;
        begy = -15;
        endx = 0;
        endy = 15;
        g.drawLine(x0 + begx, y0 + begy, x0 + endx, y0 + endy);
    } // end draw

} // end class
