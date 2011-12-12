/*
 * $Id: PolyGlyph.java,v 1.11 2005/05/13 18:32:10 jeffmc Exp $
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

import java.util.ArrayList;
import java.util.List;


/**
 * Class PolyGlyph
 *
 *
 * @author IDV development team
 */
public class PolyGlyph extends Glyph {

    /** _more_ */
    Rectangle bounds = null;

    /** _more_ */
    List points = new ArrayList();

    /** _more_ */
    int[] xs = null;

    /** _more_ */
    int[] ys = null;


    /**
     * _more_
     *
     */
    public PolyGlyph() {
        super();
    }

    /**
     * _more_
     *
     * @param canvas
     * @param event
     * @param x
     * @param y
     * @return _more_
     */
    public CanvasCommand getCreateCommand(DisplayCanvas canvas,
                                          AWTEvent event, int x, int y) {
        if (getStretchy()) {
            return new PolyGlyphCreator(canvas, event, this, x, y);
        }
        for (int i = 0; i < points.size(); i++) {
            Point p = (Point) points.get(i);
            p.x += x;
            p.y += y;
        }

        return super.getCreateCommand(canvas, event, x, y);
    }


    /**
     * _more_
     * @return _more_
     *
     * @throws CloneNotSupportedException
     */
    protected Object clone() throws CloneNotSupportedException {
        PolyGlyph clonedObject = (PolyGlyph) super.clone();
        clonedObject.points = new ArrayList();
        for (int i = 0; i < points.size(); i++) {
            Point p = (Point) points.get(i);
            clonedObject.points.add(new Point(p.x, p.y));
        }
        clonedObject.invalidatePosition();
        return clonedObject;
    }


    /**
     * _more_
     *
     * @param c
     */
    void makeArray(DisplayCanvas c) {
        if (xs != null) {
            return;
        }
        int size = points.size();
        xs = new int[size];
        ys = new int[size];

        for (int i = 0; i < size; i++) {
            Point p = (Point) points.get(i);
            xs[i] = transformOutputX(c, p.x);
            ys[i] = transformOutputY(c, p.y);
        }

    }

    /**
     * _more_
     */
    void invalidatePosition() {
        bounds = null;
        xs     = null;
        ys     = null;
    }


    /**
     * _more_
     * @return _more_
     */
    public Rectangle getBounds() {
        if (bounds == null) {
            bounds = new Rectangle();
            if (points.size() == 0) {
                return bounds;
            }
            int minx = Integer.MAX_VALUE;
            int miny = Integer.MAX_VALUE;
            int maxx = Integer.MIN_VALUE;
            int maxy = Integer.MIN_VALUE;
            for (int i = 0; i < points.size(); i++) {
                Point p = (Point) points.get(i);
                if (p.x < minx) {
                    minx = p.x;
                } else if (p.x > maxx) {
                    maxx = p.x;
                }
                if (p.y < miny) {
                    miny = p.y;
                } else if (p.y > maxy) {
                    maxy = p.y;
                }
            }
            bounds.x      = minx;
            bounds.y      = miny;
            bounds.width  = maxx - minx;
            bounds.height = maxy - miny;
        }
        return bounds;
    }

    /**
     * _more_
     *
     * @param p
     * @param cnt
     */
    public void setPoints(int[] p, int cnt) {
        invalidatePosition();
        points = new ArrayList();
        for (int i = 0; i < cnt; i += 2) {
            points.add(new Point(p[i], p[i + 1]));
        }
    }

    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paintHighlight(Graphics g, DisplayCanvas c) {
        g.setColor(highlightColor);
        paintLines(g, c, getLineWidth(), false);
    }


    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paint(Graphics g, DisplayCanvas c) {
        Color bgColor = getBackground(getForeground());
        if (getFilled()) {
            g.setColor(bgColor);
            paintLines(g, c, getLineWidth(), true);
            if (bgColor.equals(getForeground())) {
                return;
            }
        }
        g.setColor(getForeground());
        paintLines(g, c, getLineWidth(), false);
    }



    /**
     * _more_
     *
     * @param g
     * @param c
     * @param w
     * @param fill
     */
    public void paintLines(Graphics g, DisplayCanvas c, int w, boolean fill) {
        invalidatePosition();
        makeArray(c);
        if (fill) {
            g.fillPolygon(xs, ys, xs.length);
        } else {
            //      try {
            //  g.drawPolyline(xs,ys,xs.length);
            //      } catch (Throwable exc){
            int[] thex  = xs;
            int[] they  = ys;

            int   lastx = thex[0];
            int   lasty = they[0];
            int   sw    = w;
            for (int i = 0; i < thex.length; i++) {
                //TODO: Use the width
                g.drawLine(lastx, lasty, thex[i], they[i]);
                lastx = thex[i];
                lasty = they[i];
            }
            //      }
        }
    }


    /**
     * _more_
     *
     * @param x
     * @param y
     */
    public void addPoint(int x, int y) {
        invalidatePosition();
        if (points.size() > 1) {
            Point   p1       = (Point) points.get(points.size() - 1);
            Point   p2       = (Point) points.get(points.size() - 2);
            boolean extended = false;

            if ((p1.x == p2.x) && (p2.x == x)) {
                p1.y     = y;
                extended = true;
            }
            if ((p1.y == p2.y) && (p2.y == y)) {
                p1.x     = x;
                extended = true;
            }
            if (extended) {
                return;
            }
        }

        points.add(new Point(x, y));
    }


    /**
     * _more_
     * @return _more_
     */
    public String getPositionAttr() {
        String ret  = "";
        int    size = points.size();
        for (int i = 0; i < size; i++) {
            Point p = (Point) points.get(i);
            if (i != 0) {
                ret += ",";
            }
            ret = ret + "," + p.x + "," + p.y;
        }
        return makeAttr(ATTR_PTS, ret);
    }


    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public double distance(int x, int y) {
        int minIdx = findMinIndex(x, y, Double.MAX_VALUE);
        if (minIdx < 0) {
            return Double.MAX_VALUE;
        }
        return distance((Point) points.get(minIdx), new Point(x, y));
    }



    /**
     * _more_
     *
     * @param x
     * @param y
     */
    public void moveBy(int x, int y) {
        invalidatePosition();
        int size = points.size();
        for (int i = 0; i < size; i++) {
            Point p = (Point) points.get(i);
            p.x += x;
            p.y += y;
        }
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

    /**
     * _more_
     *
     * @param x
     * @param y
     * @param threshold
     * @return _more_
     */
    public int findMinIndex(int x, int y, double threshold) {
        double min    = threshold;
        int    minIdx = -1;
        Point  fp     = new Point(x, y);
        for (int i = 0; i < points.size(); i++) {
            double tmp = distance((Point) points.get(i), fp);
            if (tmp < min) {
                min    = tmp;
                minIdx = i;
            }
        }
        return minIdx;
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public String getStretchPoint(int x, int y) {
        if ( !getStretchy()) {
            return PT_CENTER;
        }
        int minIdx = findMinIndex(x, y, 2.0);
        if (minIdx >= 0) {
            return PT_PREFIX + minIdx;
        }
        return PT_CENTER;
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
        if (pt.equals(PT_CENTER)) {
            super.stretchTo(x, y, pt, correct);
        } else {
            int   index = new Integer(pt.substring(1)).intValue();
            Point p     = (Point) points.get(index);
            p.x = x;
            p.y = y;
        }
        invalidatePosition();
        return pt;
    }



    /**
     * _more_
     *
     * @param p1
     * @param p2
     * @return _more_
     */
    public static double distance(Point p1, Point p2) {
        return Math.sqrt((double) ((p2.x - p1.x) * (p2.x - p1.x)
                                   + (p2.y - p1.y) * (p2.y - p1.y)));
    }




    /**
     * _more_
     *
     * @param g
     * @param xs
     * @param ys
     */
    public static void drawPolyLine(Graphics g, int[] xs, int[] ys) {
        int i;
        for (i = 1; i < xs.length; i++) {
            g.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
        }
        g.drawLine(xs[i - 1], ys[i - 1], xs[0], ys[0]);

    }








}





