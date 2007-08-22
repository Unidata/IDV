/*
 * $Id: ShapeSymbol.java,v 1.25 2007/05/22 20:00:23 jeffmc Exp $
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

package ucar.unidata.ui.symbol;


import org.w3c.dom.Element;

import ucar.unidata.data.point.PointOb;
import ucar.unidata.ui.drawing.DisplayCanvas;


import ucar.unidata.ui.drawing.Glyph;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.ShapeUtility;


import visad.*;

import java.awt.*;

import java.awt.event.*;
import java.awt.geom.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Shape symbol.
 * @author Metapps development team
 * @version $Revision: 1.25 $
 */
public class ShapeSymbol extends MetSymbol {

    /** Xml attr name for points */
    public static final String ATTR_POINTS = "points";

    /** Xml attr name for shape */
    public static final String ATTR_SHAPE = "shape";

    /** Xml attr name for shape type (triangle, quad, line) */
    public static final String ATTR_SHAPETYPE = "shapetype";


    /** Shape type to create when using points - visadlinearray */
    public static final String TYPE_LINE = "line";

    /** Shape type to create when using points - visadtrianglearray */
    public static final String TYPE_TRIANGLE = "triangle";

    /** Shape type to create when using points - visadquadarray */
    public static final String TYPE_QUAD = "quad";


    /** Used for doing the canvas drawing */
    private VisADGeometryArray sample;


    /** Combobox for the shape in the properties dialog */
    private JComboBox shapeBox;


    /** The shape. One of the fixed ones from ucar.visad.ShapeUtility */
    private String shape;


    /** The point array from the xml */
    float[] points;

    /** The shape type from the xml. One of TYPE_LINE, TYPE_QUAD, TYPE_TRIANGLE. */
    String shapeType;

    /** Can this shape resize */
    boolean stretchy = true;

    /**
     * Default constructor.
     */
    public ShapeSymbol() {}


    /**
     * Construct a ShapeSymbol without a canvas at the
     * position specified.  Use the parameter names and long names specified.
     * @param x              x position
     * @param y              y position
     */
    public ShapeSymbol(int x, int y) {
        this(null, x, y);
    }


    /**
     * Construct a ShapeSymbol to use on the canvas specified at the
     * position specified.  Use the parameter names and long names specified.
     * @param canvas  canvas to draw on.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     */
    public ShapeSymbol(DisplayCanvas canvas, int x, int y) {
        super(canvas, x, y, new String[] {}, new String[] {});
        //rectPoint = PT_MM;
        setSize(20, 20);
    }

    /**
     * Get the label to show the user what I am in the properties
     *
     * @return label
     */
    public String getLabel() {
        return "Shape Symbol: " + shape;
    }



    /**
     * Initialize the shape with the xml
     *
     * @param symbolNode The xml
     */
    public void initialize(Element symbolNode) {
        super.initialize(symbolNode);
        stretchy = XmlUtil.getAttribute(symbolNode, "stretchy", true);
        setScale(XmlUtil.getAttribute(symbolNode, "scale", 1.0));
        shape = XmlUtil.getAttribute(symbolNode, ATTR_SHAPE,
                                     ShapeUtility.SQUARE);
        shapeType = XmlUtil.getAttribute(symbolNode, ATTR_SHAPETYPE,
                                         TYPE_LINE);
        String pointsString = XmlUtil.getAttribute(symbolNode, ATTR_POINTS,
                                  (String) null);
        if (pointsString != null) {
            points = Misc.parseFloats(pointsString);
        }
    }


    /**
     * Add any components to the list of widgets for the main property dialog
     * tab.
     *
     * @param comps List of components.
     */
    protected void initPropertyComponents(List comps) {
        super.initPropertyComponents(comps);
        if (points != null) {
            return;
        }
        makeShapePropertyComponents(comps);
    }


    /**
     * Add to the property components.
     *
     * @param comps List of label/component pairs.
     */
    protected void makeShapePropertyComponents(List comps) {
        shapeBox = new JComboBox(ShapeUtility.SHAPES);
        TwoFacedObject tfo   = new TwoFacedObject(shape, shape);
        int            index = Misc.toList(ShapeUtility.SHAPES).indexOf(tfo);
        if (index >= 0) {
            shapeBox.setSelectedIndex(index);
        } else {
            shapeBox.setSelectedItem(tfo);
        }
        comps.add(GuiUtils.rLabel("Shape:"));
        comps.add(GuiUtils.left(shapeBox));
    }


    /**
     * Apply properties from the properties dialog.
     */
    protected void applyProperties() {
        sample = null;
        if (shapeBox != null) {
            shape = (String) TwoFacedObject.getIdString(
                shapeBox.getSelectedItem());
        }
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
     *
     * @param s Is this stretchy
     */
    public void setStretchy(boolean s) {
        stretchy = s;
    }



    /**
     * Get the parameter value at the index specified.
     * @param  index  index into param array
     * @return value of uOrSpeedParam if index = 0, otherwise vOrDirParam value
     */
    public Object getParamValue(int index) {
        return null;
    }


    /**
     * Set the parameter value at the index specified.
     * @param  index  index into param array
     * @param  v      value (<code>String</code>) of double parameter value
     */
    public void setParamValue(int index, Object v) {
        if (index == 0) {}
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
     * draw the symbol at the specified location
     * @param g Graphics2D object
     * @param x  x position of the reference point
     * @param y  y position of the reference point
     * @param width  width of the symbol
     * @param height height of the symbol
     */
    public void draw(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(getForeground());
        if (sample == null) {
            sample = makeShapes(null)[0];
        }
        double  scale = getScale() * 10.0;
        float[] pts   = sample.coordinates;
        int     mx    = x + width / 2;
        int     my    = y + width / 2;
        if (sample instanceof VisADLineArray) {
            for (int i = 0; i < pts.length - 6; i += 6) {
                g.drawLine(mx + (int) (scale * pts[i]),
                           my - (int) (scale * pts[i + 1]),
                           mx + (int) (scale * pts[i + 3]),
                           my - (int) (scale * pts[i + 3 + 1]));
            }
        } else if (sample instanceof VisADTriangleArray) {
            int[] xpoints = { 0, 0, 0 };
            int[] ypoints = { 0, 0, 0 };

            for (int i = 0; i < pts.length; i += 9) {
                xpoints[0] = mx + (int) (scale * pts[i]);
                xpoints[1] = mx + (int) (scale * pts[i + 3]);
                xpoints[2] = mx + (int) (scale * pts[i + 6]);
                ypoints[0] = my - (int) (scale * pts[i + 1]);
                ypoints[1] = my - (int) (scale * pts[i + 1 + 3]);
                ypoints[2] = my - (int) (scale * pts[i + 1 + 6]);
                g.fillPolygon(xpoints, ypoints, 3);
            }
        } else if (sample instanceof VisADQuadArray) {
            int[] xpoints = { 0, 0, 0, 0 };
            int[] ypoints = { 0, 0, 0, 0 };

            for (int i = 0; i < pts.length; i += 12) {
                xpoints[0] = mx + (int) (scale * pts[i]);
                xpoints[1] = mx + (int) (scale * pts[i + 3]);
                xpoints[2] = mx + (int) (scale * pts[i + 6]);
                xpoints[3] = mx + (int) (scale * pts[i + 9]);
                ypoints[0] = my - (int) (scale * pts[i + 1]);
                ypoints[1] = my - (int) (scale * pts[i + 1 + 3]);
                ypoints[2] = my - (int) (scale * pts[i + 1 + 6]);
                ypoints[3] = my - (int) (scale * pts[i + 1 + 9]);
                g.fillPolygon(xpoints, ypoints, 4);
            }
        } else {
            g.drawRect(x, y, width, height);
        }

    }



    /**
     *  Set the Shape property.
     *
     *  @param value The new value for Shape
     */
    public void setShape(String value) {
        shape = value;
    }

    /**
     *  Get the Shape property.
     *
     *  @return The Shape
     */
    public String getShape() {
        return shape;
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
     * Make the shapes.
     *
     * @param ob The observation. We don't use this.
     *
     * @return The array of shapes.
     */
    public VisADGeometryArray[] makeShapes(PointOb ob) {
        //TODO: Draw the points
        if (points != null) {
            VisADGeometryArray shape = null;
            if (shapeType.equals(TYPE_LINE)) {
                shape = new VisADLineArray();
            } else if (shapeType.equals(TYPE_TRIANGLE)) {
                shape = new VisADTriangleArray();
            } else if (shapeType.equals(TYPE_QUAD)) {
                shape = new VisADQuadArray();
            } else {
                throw new IllegalStateException("Unknown shape:" + shapeType);
            }
            shape.coordinates = (float[]) points.clone();
            shape.vertexCount = shape.coordinates.length / 3;
            return new VisADGeometryArray[] { shape };
        }
        return ShapeUtility.createShape(shape);
    }


    /**
     * Get the shape type.
     *
     * @return The shape type
     */
    public String getShapeType() {
        return shapeType;
    }

    /**
     * Set the shape type
     *
     * @param type The shape type
     */
    public void setShapeType(String type) {
        shapeType = type;
    }


    /**
     * Get the points. From the xml.
     *
     * @return The points. May be null.
     */
    public float[] getPoints() {
        return points;
    }

    /**
     * Set the points.
     *
     * @param p The points.
     */
    public void setPoints(float[] p) {
        points = p;
    }




}

