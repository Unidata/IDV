/*
 * $Id: RectangleGlyph.java,v 1.13 2005/11/09 16:43:05 jeffmc Exp $
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
import ucar.unidata.util.Misc;


import java.awt.Point;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;


/**
 * Class RectangleGlyph
 *
 *
 * @author IDV development team
 */
public class RectangleGlyph extends Glyph {

    /** _more_ */
    public static final String RECTANGLE = "RECTANGLE";

    /** _more_ */
    public static final String FRECTANGLE = "FRECTANGLE";

    /** _more_ */
    public static final String ROUNDRECT = "ROUNDRECT";

    /** _more_ */
    public static final String FROUNDRECT = "FROUNDRECT";

    /** _more_ */
    public static final String CIRCLE = "CIRCLE";

    /** _more_ */
    public static final String FCIRCLE = "FCIRCLE";

    /** _more_ */
    public static final String ATTR_RECTTYPE = "recttype";


    /** _more_ */
    public Rectangle bounds;

    /** _more_ */
    public static final int TYPE_RECT = 0;

    /** _more_ */
    public static final int TYPE_RRECT = 1;

    /** _more_ */
    public static final int TYPE_CIRCLE = 2;

    /** _more_ */
    String typeName;

    /** _more_ */
    int type;


    /**
     * _more_
     *
     */
    public RectangleGlyph() {
        bounds = new Rectangle(0, 0, 0, 0);
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     *
     */
    public RectangleGlyph(int x, int y) {
        this(RECTANGLE, x, y, 1, 1);
    }

    /**
     * _more_
     *
     * @param typeName
     * @param x
     * @param y
     * @param w
     * @param h
     *
     */
    public RectangleGlyph(String typeName, int x, int y, int w, int h) {
        bounds = new Rectangle(x, y, w, h);
        setType(typeName);
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean getEqualSides() {
        return false;
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean getSymetricReshape() {
        return false;
    }

    /**
     * _more_
     *
     * @param typeName
     */
    protected void setType(String typeName) {
        this.typeName = typeName;
        if (typeName.equals(RECTANGLE)) {
            type = TYPE_RECT;
        } else if (typeName.equals(CIRCLE)) {
            type = TYPE_CIRCLE;
        } else if (typeName.equals(ROUNDRECT)) {
            type = TYPE_RRECT;
        } else if (typeName.equals(FRECTANGLE)) {
            type = TYPE_RECT;
            setFilled(true);
        } else if (typeName.equals(FCIRCLE)) {
            type = TYPE_CIRCLE;
            setFilled(true);
        } else if (typeName.equals(FROUNDRECT)) {
            type = TYPE_RRECT;
            setFilled(true);
        }
    }


    /**
     * _more_
     * @return _more_
     *
     * @throws CloneNotSupportedException
     */
    protected Object clone() throws CloneNotSupportedException {
        RectangleGlyph clonedObject = (RectangleGlyph) super.clone();
        clonedObject.bounds = new Rectangle(bounds.x, bounds.y, bounds.width,
                                            bounds.height);
        clonedObject.typeName = typeName;
        clonedObject.type     = type;
        return clonedObject;
    }

    /**
     * _more_
     * @return _more_
     */
    public Rectangle getBounds() {
        return bounds;
    }


    /**
     * _more_
     * @return _more_
     */
    public int getBottom() {
        return bounds.y + bounds.height;
    }

    /**
     * _more_
     * @return _more_
     */
    public int getTop() {
        return bounds.y;
    }

    /**
     * _more_
     * @return _more_
     */
    public int getLeft() {
        return bounds.x;
    }



    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paint(Graphics g, DisplayCanvas c) {
        super.paint(g, c);
        Rectangle r = transformOutput(c, bounds);
        g.setColor(getForeground());
        switch (type) {

          case TYPE_RECT : {
              if (getFilled()) {
                  g.setColor(getBackground(getForeground()));
                  g.fillRect(r.x, r.y, r.width + 1, r.height + 1);
                  g.setColor(getForeground());
              }
              g.drawRect(r.x, r.y, r.width, r.height);
              break;
          }
          case TYPE_RRECT : {
              int radius = 10;
              if (getFilled()) {
                  g.setColor(getBackground(getForeground()));
                  g.fillRoundRect(r.x, r.y, r.width + 1, r.height + 1,
                                  radius, radius);
                  g.setColor(getForeground());
              }
              g.drawRoundRect(r.x, r.y, r.width, r.height, radius, radius);
              break;
          }
          case TYPE_CIRCLE : {
              if (getFilled()) {
                  g.setColor(getBackground(getForeground()));
                  g.fillArc(r.x, r.y, r.width + 1, r.height + 1, 0, 360);
                  g.setColor(getForeground());
              }
              g.drawArc(r.x, r.y, r.width, r.height, 0, 360);
              break;
          }
        }
    }


    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public double distance(int x, int y) {
        boolean inx = ((x > bounds.x) && (x < bounds.x + bounds.width));
        boolean iny = ((y > bounds.y) && (y < bounds.y + bounds.height));

        if (inx && iny) {
            if (getFilled()) {
                return 0.0;
            }
            double d1 = (x - bounds.x);
            double d2 = (bounds.x + bounds.width - x);
            double d3 = (y - bounds.y);
            double d4 = (bounds.y + bounds.height - y);
            return Math.min(d1, Math.min(d2, Math.min(d3, d4)));
        }


        if (inx) {
            if (y < bounds.y) {
                return (bounds.y - y);
            }
            return (y - (bounds.y + bounds.height));
        }
        if (iny) {
            if (x < bounds.x) {
                return (bounds.x - x);
            }
            return (x - (bounds.x + bounds.width));
        }


        int cx = 0;
        int cy = 0;

        if (x < bounds.x) {
            cx = bounds.x;
        } else if (x > (bounds.x + bounds.width)) {
            cx = bounds.x + bounds.width;
        } else {
            cx = bounds.x + (bounds.width / 2);
        }

        if (y < bounds.y) {
            cy = bounds.y;
        } else if (y > (bounds.y + bounds.height)) {
            cy = bounds.y + bounds.height;
        } else {
            cy = bounds.y + (bounds.height / 2);
        }

        return (Math.sqrt((cx - x) * (cx - x) + (cy - y) * (cy - y)));
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

        if ( !getStretchy()) {
            pt = PT_CENTER;
        }

        if (PT_CENTER.equals(pt)) {
            bounds.x += x;
            bounds.y += y;
            boundsChanged();
            return pt;
        }

        String  vs     = pt.substring(0, 1);
        String  hs     = pt.substring(1, 2);
        boolean es     = getEqualSides();
        boolean sym    = getSymetricReshape();
        int     vdelta = 0;
        int     hdelta = 0;
        boolean didOV  = false;
        boolean didOH  = false;
        boolean didV   = false;
        boolean didH   = false;
        int     ox     = bounds.x;
        int     oy     = bounds.y;
        int     cx     = bounds.x + bounds.width / 2;
        int     cy     = bounds.y + bounds.height / 2;

        if (vs.equals(PT_V_UPPER)) {
            didOV         = true;
            vdelta        = (bounds.y - y);
            didV          = true;
            bounds.height += vdelta;
            bounds.y      = y;
            if (sym) {
                bounds.height += vdelta;
            }
        } else if (vs.equals(PT_V_LOWER)) {
            vdelta        = (y - bounds.y) - bounds.height;
            didV          = true;
            bounds.height += vdelta;
            if (sym) {
                bounds.y      -= vdelta;
                bounds.height += vdelta;
            }
        }

        if (hs.equals(PT_H_LEFT)) {
            didOH        = true;
            hdelta       = (bounds.x - x);
            didH         = true;
            bounds.width += hdelta;
            bounds.x     = x;
            if (sym) {
                bounds.width += hdelta;
            }
        } else if (hs.equals(PT_H_RIGHT)) {
            hdelta       = (x - bounds.x) - bounds.width;
            didH         = true;
            bounds.width += hdelta;
            if (sym) {
                bounds.x     -= hdelta;
                bounds.width += hdelta;
            }
        }

        if (es && (didV || didH)) {
            if ( !didH) {
                bounds.width = bounds.height;
            } else if ( !didV) {
                bounds.height = bounds.width;
            } else {
                if (bounds.width < bounds.height) {
                    if (didOH) {
                        bounds.x += (bounds.width - bounds.height);
                    }
                    bounds.width = bounds.height;
                } else {
                    if (didOV) {
                        bounds.y += (bounds.height - bounds.width);
                    }
                    bounds.height = bounds.width;
                }
            }
        }

        if (sym) {
            bounds.x += (cx - (bounds.x + bounds.width / 2));
            bounds.y += (cy - (bounds.y + bounds.height / 2));
        }

        if (correct) {
            if (bounds.width <= 0) {
                if (PT_H_LEFT.equals(hs)) {
                    hs = PT_H_RIGHT;
                } else {
                    hs = PT_H_LEFT;
                }
                bounds.x     = bounds.x + bounds.width;
                bounds.width = -bounds.width;
            }
            if (bounds.height <= 0) {
                if (PT_V_UPPER.equals(vs)) {
                    vs = PT_V_LOWER;
                } else {
                    vs = PT_V_UPPER;
                }
                bounds.y      = bounds.y + bounds.height;
                bounds.height = -bounds.height;
            }
        }

        boundsChanged();

        return vs + hs;
    }



    /**
     * _more_
     *
     * @param pt
     * @param b
     * @return _more_
     */
    public static Point getPoint(String pt, Rectangle b) {
        Point  p  = new Point(b.x, b.y);
        String vs = pt.substring(0, 1);
        String hs = pt.substring(1, 2);
        if (vs.equals(PT_V_UPPER)) {
            p.y = b.y;
        } else if (vs.equals(PT_V_MIDDLE)) {
            p.y = b.y + b.height / 2;
        } else if (vs.equals(PT_V_LOWER)) {
            p.y = b.y + b.height;
        }

        if (hs.equals(PT_H_LEFT)) {
            p.x = b.x;
        } else if (hs.equals(PT_H_MIDDLE)) {
            p.x = b.x + b.width / 2;
        } else if (hs.equals(PT_H_RIGHT)) {
            p.x = b.x + b.width;
        }

        return p;
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
	return getStretchPoint(bounds,x,y);
    }


    /**
     * _more_
     *
     * @param x
     * @param y
     */
    public void moveBy(int x, int y) {
        moveTo(x + bounds.x, y + bounds.y);
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     */
    public void moveTo(int x, int y) {
        bounds.x = x;
        bounds.y = y;
    }



    /**
     * _more_
     *
     * @param p
     * @param cnt
     */
    public void setPoints(int[] p, int cnt) {
        moveBy(p[0] - bounds.x, p[1] - bounds.y);
        bounds.width  = p[2];
        bounds.height = p[3];
    }


    /**
     * _more_
     * @return _more_
     */
    public String getPositionAttr() {
        return makeAttr(ATTR_PTS,
                        bounds.x + "," + bounds.y + "," + bounds.width + ","
                        + bounds.height);

    }

    /**
     * _more_
     *
     * @param name
     * @param value
     */
    public void setAttr(String name, String value) {
        if (ATTR_RECTTYPE.equals(name)) {
            setType(value);
        } else if (ATTR_WIDTH.equals(name)) {
            bounds.width = Misc.getInt(value, bounds.width);
        } else if (ATTR_HEIGHT.equals(name)) {
            bounds.height = Misc.getInt(value, bounds.height);
        } else {
            super.setAttr(name, value);
        }
    }

    /**
     * _more_
     *
     * @param w
     * @param h
     */
    public void setSize(int w, int h) {
        bounds.width  = w;
        bounds.height = h;
    }


}





