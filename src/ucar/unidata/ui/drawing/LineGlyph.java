/*
 * $Id: LineGlyph.java,v 1.9 2005/05/13 18:32:10 jeffmc Exp $
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



import java.awt.Point;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;


/**
 * Class LineGlyph
 *
 *
 * @author IDV development team
 */
public class LineGlyph extends Glyph {

    /** _more_ */
    Point p1;

    /** _more_ */
    Point p2;

    /**
     * _more_
     *
     * @param x1
     * @param y1
     *
     */
    public LineGlyph(int x1, int y1) {
        this(x1, y1, x1, y1);
    }

    /**
     * _more_
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     *
     */
    public LineGlyph(int x1, int y1, int x2, int y2) {
        super();
        p1 = new Point(x1, y1);
        p2 = new Point(x2, y2);
    }

    /**
     * _more_
     * @return _more_
     *
     * @throws CloneNotSupportedException
     */
    protected Object clone() throws CloneNotSupportedException {
        LineGlyph clonedObject = (LineGlyph) super.clone();
        clonedObject.p1 = new Point(p1.x, p1.y);
        clonedObject.p2 = new Point(p2.x, p2.y);
        return clonedObject;
    }


    /**
     * _more_
     * @return _more_
     */
    public Rectangle getBounds() {
        Rectangle r = new Rectangle();
        if (p1.x < p2.x) {
            r.x     = p1.x;
            r.width = (p2.x - p1.x);
        } else {
            r.x     = p2.x;
            r.width = (p1.x - p2.x);
        }
        if (p1.y < p2.y) {
            r.y      = p1.y;
            r.height = (p2.y - p1.y);
        } else {
            r.y      = p2.y;
            r.height = (p1.y - p2.y);
        }

        return r;

    }

    /**
     * _more_
     *
     * @param p
     * @param cnt
     */
    public void setPoints(int[] p, int cnt) {
        if (p.length < 4) {
            return;
        }
        p1.x = p[0];
        p1.y = p[1];
        p2.x = p[2];
        p2.y = p[3];
    }

    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paint(Graphics g, DisplayCanvas c) {
        paint(g, c, getForeground());
    }

    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paintHighlight(Graphics g, DisplayCanvas c) {
        paint(g, c, highlightColor);
    }

    /**
     * _more_
     *
     * @param g
     * @param c
     * @param color
     */
    public void paint(Graphics g, DisplayCanvas c, Color color) {
        Point sp1 = transformOutput(c, p1);
        Point sp2 = transformOutput(c, p2);
        g.setColor(color);
        g.drawLine(sp1.x, sp1.y, sp2.x, sp2.y);
        g.setColor(Color.black);
    }


    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paintSelection(Graphics g, DisplayCanvas c) {
        Point sp1 = transformOutput(c, p1);
        Point sp2 = transformOutput(c, p2);
        g.setColor(Color.black);
        g.fillRect(sp1.x - H_SEL_WIDTH, sp1.y - H_SEL_WIDTH, SEL_WIDTH,
                   SEL_WIDTH);
        g.fillRect(sp2.x - H_SEL_WIDTH, sp2.y - H_SEL_WIDTH, SEL_WIDTH,
                   SEL_WIDTH);
    }


    /**
     * _more_
     * @return _more_
     */
    public String getPositionAttr() {
        return makeAttr(ATTR_PTS,
                        p1.x + "," + p1.y + "," + p2.x + "," + p2.y);
    }


    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public double distance(int x, int y) {
        return ptSegDist((double) p1.x, (double) p1.y, (double) p2.x,
                         (double) p2.y, (double) x, (double) y);

    }


    /**
     * _more_
     *
     * @param x
     * @param y
     * @param pt
     * @param correct
     * @return _more_
     */
    public String stretchTo(int x, int y, String pt, boolean correct) {
        if (PT_P1.equals(pt) || PT_UL.equals(pt)) {
            p1.x = x;
            p1.y = y;
        } else if (PT_P2.equals(pt) || PT_LR.equals(pt)) {
            p2.x = x;
            p2.y = y;
        } else {
            p1.x += x;
            p1.y += y;
            p2.x += x;
            p2.y += y;
        }


        return pt;
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public String getStretchPoint(int x, int y) {
        double d1  = distance(x, y, p1.x, p1.y);
        double d2  = distance(x, y, p2.x, p2.y);

        double min = ((d1 <= d2)
                      ? d1
                      : d2);
        if (min > MIN_DISTANCE_TO_STRETCH) {
            return PT_CENTER;
        }
        if (d1 <= d2) {
            return PT_P1;
        }
        return PT_P2;
    }


    /**
     * _more_
     *
     * @param x
     * @param y
     */
    public void moveBy(int x, int y) {
        p1.x += x;
        p1.y += y;
        p2.x += x;
        p2.y += y;
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     */
    public void moveTo(int x, int y) {
        //TODO
    }


    //Taken from java1.2 java.awt.geom.Line2D

    /**
     * _more_
     *
     * @param X1
     * @param Y1
     * @param X2
     * @param Y2
     * @param PX
     * @param PY
     * @return _more_
     */
    public static double ptSegDistSq(double X1, double Y1, double X2,
                                     double Y2, double PX, double PY) {
        // Adjust vectors relative to X1,Y1
        // X2,Y2 becomes relative vector from X1,Y1 to end of segment
        X2 -= X1;
        Y2 -= Y1;
        // PX,PY becomes relative vector from X1,Y1 to test point
        PX -= X1;
        PY -= Y1;
        double dotprod = PX * X2 + PY * Y2;
        double projlenSq;
        if (dotprod <= 0.0) {
            // PX,PY is on the side of X1,Y1 away from X2,Y2
            // distance to segment is length of PX,PY vector
            // "length of its (clipped) projection" is now 0.0
            projlenSq = 0.0;
        } else {
            // switch to backwards vectors relative to X2,Y2
            // X2,Y2 are already the negative of X1,Y1=>X2,Y2
            // to get PX,PY to be the negative of PX,PY=>X2,Y2
            // the dot product of two negated vectors is the same
            // as the dot product of the two normal vectors
            PX      = X2 - PX;
            PY      = Y2 - PY;
            dotprod = PX * X2 + PY * Y2;
            if (dotprod <= 0.0) {
                // PX,PY is on the side of X2,Y2 away from X1,Y1
                // distance to segment is length of (backwards) PX,PY vector
                // "length of its (clipped) projection" is now 0.0
                projlenSq = 0.0;
            } else {
                // PX,PY is between X1,Y1 and X2,Y2
                // dotprod is the length of the PX,PY vector
                // projected on the X2,Y2=>X1,Y1 vector times the
                // length of the X2,Y2=>X1,Y1 vector
                projlenSq = dotprod * dotprod / (X2 * X2 + Y2 * Y2);
            }
        }
        // Distance to line is now the length of the relative point
        // vector minus the length of its projection onto the line
        // (which is zero if the projection falls outside the range
        //  of the line segment).
        return PX * PX + PY * PY - projlenSq;
    }

    /**
     * _more_
     *
     * @param X1
     * @param Y1
     * @param X2
     * @param Y2
     * @param PX
     * @param PY
     * @return _more_
     */
    public static double ptSegDist(double X1, double Y1, double X2,
                                   double Y2, double PX, double PY) {
        return Math.sqrt(ptSegDistSq(X1, Y1, X2, Y2, PX, PY));
    }


}





