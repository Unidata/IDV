/*
 * $Id: TestCanvas.java,v 1.7 2005/05/13 18:32:11 jeffmc Exp $
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


package ucar.unidata.ui.drawing;



import ucar.unidata.util.GuiUtils;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 */

public class TestCanvas extends EditCanvas {

    /**
     * _more_
     *
     */
    public TestCanvas() {}


    /**
     *  List of the names of the shapes we use.
     */
    static String[] shapeNames = {
        "Rectangle", "R.Rect", "Circle", "Line", "Image", "Polygon"
    };


    /**
     *  List of the types of shapes we use
     */
    static String[] shapeTypes = {
        "ucar.unidata.ui.drawing.RectangleGlyph",
        "ucar.unidata.ui.drawing.RectangleGlyph",
        "ucar.unidata.ui.drawing.RectangleGlyph",
        "ucar.unidata.ui.drawing.LineGlyph",
        "ucar.unidata.ui.drawing.ImageGlyph",
        "ucar.unidata.ui.drawing.PolyGlyph"
    };

    /** _more_ */
    static String[] shapeAttrs = {
        "", "type=ROUNDRECT;width=50;height=50;stretchy=false;",
        "type=CIRCLE", "", "image=/ucar/unidata/ui/drawing/images/text.gif",
        "pts=20,0,0,20,20,20,20,0;stretchy=false"
    };


    /**
     *  Icons for the shape palette
     */
    static String[] shapeIcons = {
        "/ucar/unidata/ui/drawing/images/rect.gif",
        "/ucar/unidata/ui/drawing/images/rrect.gif",
        "/ucar/unidata/ui/drawing/images/circle.gif",
        "/ucar/unidata/ui/drawing/images/line.gif",
        "/ucar/unidata/ui/drawing/images/image.gif",
        "/ucar/unidata/ui/drawing/images/poly.gif"
    };

    /**
     * _more_
     * @return _more_
     */
    public String[] getShapeAttrs() {
        return shapeAttrs;
    }

    /**
     * _more_
     * @return _more_
     */
    public String[] getShapeNames() {
        return shapeNames;
    }

    /**
     * _more_
     * @return _more_
     */
    public String[] getShapeTypes() {
        return shapeTypes;
    }

    /**
     * _more_
     * @return _more_
     */
    public String[] getShapeIcons() {
        return shapeIcons;
    }

    /**
     * _more_
     *
     * @param g
     */
    public void paint(Graphics g) {
        super.paint(g);
        Rectangle b = getBounds();
        g.setColor(Color.gray);
        g.drawLine(b.width / 2, 0, b.width / 2, b.height);
        g.drawLine(0, b.height / 2, b.width, b.height / 2);
    }


    /**
     * _more_
     * @return _more_
     */
    public Point getCenter() {
        Rectangle b = getBounds();
        return new Point(b.width / 2, b.height / 2);
    }

    /**
     * _more_
     *
     * @param x
     * @return _more_
     */
    public int transformInputX(int x) {
        Point center = getCenter();
        return x - center.x;
    }

    /**
     * _more_
     *
     * @param y
     * @return _more_
     */
    public int transformInputY(int y) {
        Point center = getCenter();
        return y - center.y;
    }

    /**
     * _more_
     *
     * @param x
     * @return _more_
     */
    public int transformOutputX(int x) {
        Point center = getCenter();
        return center.x + x;
    }

    /**
     * _more_
     *
     * @param y
     * @return _more_
     */
    public int transformOutputY(int y) {
        Point center = getCenter();
        return center.y + y;
    }

    /**
     * _more_
     *
     * @param r
     * @return _more_
     */
    public Rectangle transformOutput(Rectangle r) {
        return new Rectangle(transformOutputX(r.x), transformOutputY(r.y),
                             r.width, r.height);
    }

    /**
     * _more_
     *
     * @param r
     * @return _more_
     */
    public Rectangle transformInput(Rectangle r) {
        return new Rectangle(transformInputX(r.x), transformInputY(r.y),
                             r.width, r.height);
    }

    /**
     * _more_
     *
     * @param r
     * @return _more_
     */
    public Point transformOutput(Point r) {
        return new Point(transformOutputX(r.x), transformOutputY(r.y));

    }

    /**
     * _more_
     *
     * @param r
     * @return _more_
     */
    public Point transformInput(Point r) {
        return new Point(transformInputX(r.x), transformInputY(r.y));

    }






    /**
     * _more_
     *
     * @param args
     */
    public static void main(String[] args) {
        TestCanvas c = new TestCanvas();
        c.setPreferredSize(new Dimension(400, 300));
        GuiUtils.packWindow(new JFrame(), c.getContents(), true);

    }


}








