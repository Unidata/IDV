/*
 * $Id: Glyph.java,v 1.22 2006/07/12 23:45:22 jeffmc Exp $
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


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;



import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.util.Enumeration;

import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;



/**
 *  Base (abstract) class for representing things that are drawn on
 *  the screen. Holds some basic attributes (but no positional attrs)
 *  and has a facility for recreating itself via xml.
 */

public abstract class Glyph implements Cloneable {

    /** _more_ */
    public static final String TAG_GLYPH = "glyph";

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String PT_CENTER = "MM";

    /** _more_ */
    public static final String PT_V_UPPER = "U";

    /** _more_ */
    public static final String PT_V_MIDDLE = "M";

    /** _more_ */
    public static final String PT_V_LOWER = "L";

    /** _more_ */
    public static final String PT_H_LEFT = "L";

    /** _more_ */
    public static final String PT_H_MIDDLE = "M";

    /** _more_ */
    public static final String PT_H_RIGHT = "R";

    /** _more_ */
    public static final String PT_UL = PT_V_UPPER + PT_H_LEFT;

    /** _more_ */
    public static final String PT_UM = PT_V_UPPER + PT_H_MIDDLE;

    /** _more_ */
    public static final String PT_UR = PT_V_UPPER + PT_H_RIGHT;

    /** _more_ */
    public static final String PT_ML = PT_V_MIDDLE + PT_H_LEFT;

    /** _more_ */
    public static final String PT_MM = PT_V_MIDDLE + PT_H_MIDDLE;

    /** _more_ */
    public static final String PT_MR = PT_V_MIDDLE + PT_H_RIGHT;

    /** _more_ */
    public static final String PT_LL = PT_V_LOWER + PT_H_LEFT;

    /** _more_ */
    public static final String PT_LM = PT_V_LOWER + PT_H_MIDDLE;

    /** _more_ */
    public static final String PT_LR = PT_V_LOWER + PT_H_RIGHT;

    /** _more_ */
    public static final String[] RECTPOINTS = {
        PT_UL, PT_UM, PT_UR, PT_ML, PT_MM, PT_MR, PT_LL, PT_LM, PT_LR
    };

    /** _more_ */
    public static final String[] RECTPOINTNAMES = {
        "NW", "N", "NE", "W", "Middle", "E", "SW", "S", "SE"
    };



    /** _more_ */
    public static final String PT_PREFIX = "P";

    /** _more_ */
    public static final String PT_P1 = PT_PREFIX + "1";

    /** _more_ */
    public static final String PT_P2 = PT_PREFIX + "2";



    /** _more_ */
    public static final String ATTR_CHILDREN = "children";

    /** _more_ */
    public static final String ATTR_COLOR = "color";

    /** _more_ */
    public static final String ATTR_BGCOLOR = "bgcolor";

    /** _more_ */
    public static final String ATTR_FILL = "fill";

    /** _more_ */
    public static final String ATTR_HEIGHT = "height";

    /** _more_ */
    public static final String ATTR_IMAGE = "image";

    /** _more_ */
    public static final String ATTR_LINEWIDTH = "linewidth";

    /** _more_ */
    public static final String ATTR_PARENT = "parent";

    /** _more_ */
    public static final String ATTR_PTS = "pts";

    /** _more_ */
    public static final String ATTR_STRETCHY = "stretchy";

    /** _more_ */
    public static final String ATTR_TEXT = "text";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_WIDTH = "width";

    /** _more_ */
    public static Hashtable idToGlyph = new Hashtable();


    /**
     *  Some global consts
     */
    public static final int MIN_DISTANCE_TO_STRETCH = 4;

    /** _more_ */
    public static final int SEL_WIDTH = 6;

    /** _more_ */
    public static final int H_SEL_WIDTH = 3;

    /** _more_ */
    public static final Color highlightColor = Color.yellow;




    /** _more_ */
    private boolean stretchy = true;

    /** _more_ */
    private CompositeGlyph parent;

    /** _more_ */
    String type;

    /** _more_ */
    private boolean filled = false;

    /** _more_ */
    private int lineWidth = 1;

    /** _more_ */
    private Color foregroundColor = Color.black;

    /** _more_ */
    private Color backgroundColor = null;

    /** _more_ */
    public boolean underline = false;

    /** _more_ */
    public String url;

    /** _more_ */
    public int baseline;

    /** _more_ */
    private String id = "";

    /** _more_ */
    boolean beingCreated = true;


    /**
     * _more_
     *
     * @param rectPoint _more_
     *
     * @return _more_
     */
    public static Cursor getCursor(String rectPoint) {
        if (rectPoint == null) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        if (rectPoint.equals(PT_UL)) {
            return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
        }
        if (rectPoint.equals(PT_UM)) {
            return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
        }
        if (rectPoint.equals(PT_UR)) {
            return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
        }
        if (rectPoint.equals(PT_ML)) {
            return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
        }
        if (rectPoint.equals(PT_MR)) {
            return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        }
        if (rectPoint.equals(PT_LL)) {
            return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
        }
        if (rectPoint.equals(PT_LM)) {
            return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
        }
        if (rectPoint.equals(PT_LR)) {
            return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
        }
        return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

    }


    /**
     * _more_
     *
     * @param rectPoint
     * @return _more_
     */
    public static String getRectPointName(String rectPoint) {
        for (int i = 0; i < RECTPOINTS.length; i++) {
            if (RECTPOINTS[i].equals(rectPoint)) {
                return RECTPOINTNAMES[i];
            }
        }
        return rectPoint;
    }



    /**
     *  By default paint little black rectangles at the corners and the sides
     *
     * @param g The graphics to paint into
     * @param r The rectangle to decorate
     * @param size Box size
     */

    public static void paintSelectionPoints(Graphics g, RectangularShape r,
                                            int size) {
        int halfSize = size / 2;
        int L        = (int) (r.getX() - halfSize);
        int T        = (int) (r.getY() - halfSize);
        int R        = (int) (r.getX() + r.getWidth() - halfSize + 1);
        int B        = (int) (r.getY() + r.getHeight() - halfSize + 1);
        int MY       = (int) (T + r.getHeight() / 2);
        int MX       = (int) (L + r.getWidth() / 2);
        g.fillRect(L, T, size, size);
        g.fillRect(L, B, size, size);
        g.fillRect(R, T, size, size);
        g.fillRect(R, B, size, size);
        g.fillRect(L, MY, size, size);
        g.fillRect(R, MY, size, size);
        g.fillRect(MX, T, size, size);
        g.fillRect(MX, B, size, size);
    }




    /**
     * _more_
     *
     * @param pt
     * @return _more_
     */
    public static String flipY(String pt) {
        String vs = pt.substring(0, 1);
        String hs = pt.substring(1, 2);
        if (vs.equals(PT_V_UPPER)) {
            vs = PT_V_LOWER;
        } else if (vs.equals(PT_V_LOWER)) {
            vs = PT_V_UPPER;
        }
        return vs + hs;
    }

    /**
     * _more_
     *
     * @param pt
     * @param r
     * @return _more_
     */
    public static Point2D getPointOnRect(String pt, Rectangle2D r) {
        return getPointOnRect(pt, r, new Point2D.Double());
    }


    /**
     * _more_
     *
     * @param r _more_
     *
     * @return _more_
     */
    public static Rectangle toRect(RectangularShape r) {
        return new Rectangle((int) r.getX(), (int) r.getY(),
                             (int) r.getWidth(), (int) r.getHeight());
    }



    /**
     * _more_
     *
     *
     * @param r _more_
     * @param x
     * @param y
     * @return _more_
     */
    public static String getStretchPoint(RectangularShape r, int x, int y) {
        Rectangle bounds = toRect(r);
        int       tmp;
        String    vS   = PT_V_UPPER;

        int       vMin = Math.abs(bounds.y - y);
        tmp = Math.abs(y - (bounds.y + bounds.height / 2));
        if (tmp < vMin) {
            vMin = tmp;
            vS   = PT_V_MIDDLE;
        }
        tmp = Math.abs(y - (bounds.y + bounds.height));
        if (tmp < vMin) {
            vMin = tmp;
            vS   = PT_V_LOWER;
        }

        String hS   = PT_H_LEFT;
        int    hMin = Math.abs(bounds.x - x);
        tmp = Math.abs(x - (bounds.x + bounds.width / 2));
        if (tmp < hMin) {
            hMin = tmp;
            hS   = PT_H_MIDDLE;
        }
        tmp = Math.abs(x - (bounds.x + bounds.width));
        if (tmp < hMin) {
            hMin = tmp;
            hS   = PT_H_RIGHT;
        }


        if ( !(vS.equals(PT_V_MIDDLE) && hS.equals(PT_H_MIDDLE))
                && (vMin <= MIN_DISTANCE_TO_STRETCH)
                && (hMin <= MIN_DISTANCE_TO_STRETCH)) {
            return vS + hS;
        }
        return PT_CENTER;
    }





    /**
     * _more_
     *
     * @param pt The point spec
     * @param r The rectangle
     * @param point The point to set
     * @return _more_
     */
    public static Point2D getPointOnRect(String pt, Rectangle2D r,
                                         Point2D point) {
        pt = pt.toUpperCase();
        String vs = pt.substring(0, 1);
        String hs = pt.substring(1, 2);
        double x  = 0.0;
        double y  = 0.0;

        if (vs.equals(PT_V_UPPER)) {
            y = Math.min(r.getY(), r.getY() + r.getHeight());
        } else if (vs.equals(PT_V_LOWER)) {
            y = Math.max(r.getY(), r.getY() + r.getHeight());
        } else {
            y = r.getY() + r.getHeight() / 2.0;
        }

        if (hs.equals(PT_H_LEFT)) {
            x = Math.min(r.getX(), r.getX() + r.getWidth());
        } else if (hs.equals(PT_H_RIGHT)) {
            x = Math.max(r.getX(), r.getX() + r.getWidth());
        } else {
            x = r.getX() + r.getWidth() / 2.0;
        }
        point.setLocation(x, y);
        return point;
    }




    /**
     * _more_
     *
     * @param bounds
     * @param x
     * @param y
     * @param pt
     * @return _more_
     */
    public static String stretchTo(Rectangle bounds, int x, int y,
                                   String pt) {
        return stretchTo(bounds, x, y, pt, true, false, false);
    }




    /**
     * _more_
     *
     *
     * @param bounds _more_
     * @param x
     * @param y
     * @param pt
     * @param correct
     * @param symetric _more_
     * @param equalSides _more_
     * @return _more_
     */
    public static String stretchTo(Rectangle bounds, int x, int y, String pt,
                                   boolean correct, boolean symetric,
                                   boolean equalSides) {

        if (PT_CENTER.equals(pt)) {
            bounds.x += x;
            bounds.y += y;
            return pt;
        }

        String  vs     = pt.substring(0, 1);
        String  hs     = pt.substring(1, 2);
        boolean es     = equalSides;
        boolean sym    = symetric;
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



        return vs + hs;

    }






    /**
     * _more_
     *
     */
    public Glyph() {}

    /**
     * _more_
     * @return _more_
     */
    public boolean getStretchy() {
        return stretchy;
    }

    /**
     * _more_
     *
     * @param b
     */
    public void setStretchy(boolean b) {
        stretchy = b;
    }

    /**
     * _more_
     *
     * @param b
     */
    public void setBeingCreated(boolean b) {
        beingCreated = b;
    }


    /**
     * _more_
     * @return _more_
     */
    public boolean getBeingCreated() {
        return beingCreated;
    }

    /**
     * _more_
     */
    public void initDone() {}

    /**
     * _more_
     *
     * @param newId
     */
    public void setId(String newId) {
        if ( !id.equals("")) {
            idToGlyph.remove(id);
        }
        id = newId;
        if ( !id.equals("")) {
            idToGlyph.put(id, this);
        }
    }

    /**
     * _more_
     * @return _more_
     */
    public String getId() {
        return id;
    }



    /**
     * _more_
     */
    public void doRemove() {
        if ( !id.equals("")) {
            idToGlyph.remove(id);
        }
        if (parent != null) {
            parent.removeChild(this);
        }
    }

    /**
     * _more_
     *
     * @param newParent
     */
    public void setParent(CompositeGlyph newParent) {
        parent = newParent;
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean pickable() {
        return parent == null;
    }

    /**
     * _more_
     */
    public void notifyChange() {
        if (parent != null) {
            parent.calculateBounds();
        }
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
        return new GlyphCreator(canvas, event, this, x, y);
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
    public CanvasCommand getMoveCommand(DisplayCanvas canvas, AWTEvent event,
                                        int x, int y) {
        return new GlyphStretcher(canvas, event, this, canvas.getSelection(),
                                  x, y);
    }

    /**
     * _more_
     * @return _more_
     *
     * @throws CloneNotSupportedException
     */
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }




    //These methods used  to guide the behavior of the canvas editing

    /**
     * _more_
     * @return _more_
     */
    public boolean getPersistent() {
        return true;
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean canStretch() {
        return true;
    }

    /** Hook for when the size or position of this glyph is changed */
    public void boundsChanged() {}



    /**
     * Hook for setting points from the attribute line
     *
     * @param p
     * @param cnt
     */
    public void setPoints(int[] p, int cnt) {}

    //Attribute sets and gets




    /**
     * _more_
     *
     * @param c
     */
    public void setForeground(Color c) {
        foregroundColor = c;
    }

    /**
     * _more_
     * @return _more_
     */
    public Color getForeground() {
        return foregroundColor;
    }

    /**
     * _more_
     *
     * @param c
     */
    public void setBackground(Color c) {
        backgroundColor = c;
    }

    /**
     * _more_
     * @return _more_
     */
    public Color getBackground() {
        return backgroundColor;
    }

    /**
     * _more_
     *
     * @param dflt
     * @return _more_
     */
    public Color getBackground(Color dflt) {
        return ((backgroundColor != null)
                ? backgroundColor
                : dflt);
    }

    /**
     * _more_
     *
     * @param value
     */
    public void setLineWidth(int value) {
        lineWidth = value;
    }

    /**
     * _more_
     * @return _more_
     */
    public int getLineWidth() {
        return lineWidth;
    }


    /**
     * _more_
     *
     * @param c
     */
    public void setFilled(boolean c) {
        filled = c;
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean getFilled() {
        return filled;
    }

    /**
     *  A Glyph can write out a persistent copy of itself as a set of
     *  attribute/value  pairs.
     *  This method parses the line of attr/values of the form:
     *  ATTR=VALUE;ATTR=VALUE;
     *
     * @param s
     */

    public void processAttrs(String s) {
        if (s == null) {
            return;
        }
        Properties p = Misc.parseProperties(s);
        for (Enumeration keys = p.keys(); keys.hasMoreElements(); ) {
            String attr  = (String) keys.nextElement();
            String value = (String) p.get(attr);
            setAttr(attr.trim(), value);
        }
    }


    /**
     *  set my named atttribute to value
     *
     * @param name
     * @param value
     */
    public void setAttr(String name, String value) {
        if (ATTR_FILL.equals(name)) {
            setFilled(("true".equals(value)));
        } else if (ATTR_TYPE.equals(name)) {
            type = value;
        } else if (ATTR_STRETCHY.equals(name)) {
            stretchy = Misc.getBoolean(value, stretchy);
        } else if (ATTR_LINEWIDTH.equals(name)) {
            setLineWidth(Misc.getInt(value, lineWidth));
        } else if (ATTR_BGCOLOR.equals(name)) {
            setBackground(GuiUtils.decodeColor(value, Color.black));
        } else if (ATTR_COLOR.equals(name)) {
            setForeground(GuiUtils.decodeColor(value, Color.black));
        } else if (ATTR_PTS.equals(name)) {
            StringTokenizer st  = new StringTokenizer(value, ",");
            int[]           pts = new int[1000];
            int             i   = 0;
            try {
                while (st.hasMoreTokens() && (i < pts.length)) {
                    pts[i++] = Integer.decode(st.nextToken()).intValue();
                }
            } catch (Exception exc) {}
            setPoints(pts, i);
        } else {
            //      System.err.println("Unknown Glyph attribute:" + name);
        }

    }



    /**
     *  Return the string used to recreate me
     * @return _more_
     */
    public String getXml() {
        return XmlUtil.tag(TAG_GLYPH,
                           XmlUtil.attr(ATTR_CLASS, getClass().getName())
                           + " " + getPositionAttr() + " "
                           + getAttrs()) + "\n";
    }

    /**
     * _more_
     *
     * @param name
     * @param value
     * @return _more_
     */
    public String makeAttr(String name, String value) {
        return XmlUtil.attr(name, value);
    }



    /**
     * _more_
     * @return _more_
     */
    public String getAttrs() {
        return makeAttr(ATTR_STRETCHY, "" + stretchy)
               + makeAttr(ATTR_TYPE, type) + makeAttr(ATTR_FILL, "" + filled)
               + makeAttr(ATTR_LINEWIDTH, "" + lineWidth)
               + makeAttr(ATTR_BGCOLOR, ((backgroundColor != null)
                                         ? backgroundColor.getRed() + ","
                                           + backgroundColor.getGreen() + ","
                                           + backgroundColor.getBlue()
                                         : "null")) + makeAttr(ATTR_COLOR,
                                         foregroundColor.getRed() + ","
                                         + foregroundColor.getGreen() + ","
                                         + foregroundColor.getBlue());
    }

    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paint(Graphics g, DisplayCanvas c) {}

    /**
     *  By default paint little black rectangles at the corners and the sides
     *
     * @param g
     * @param c
     */
    public void paintSelection(Graphics g, DisplayCanvas c) {
        Rectangle r = transformOutput(c, getBounds());
        if (c.canvasBg == Color.black) {
            g.setColor(Color.white);
        } else {
            g.setColor(Color.black);
        }
        paintSelectionPoints(g, r, SEL_WIDTH);
    }




    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paintHighlight(Graphics g, DisplayCanvas c) {
        Rectangle r = transformOutput(c, getBounds());
        g.setColor(highlightColor);
        g.drawRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
        g.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
    }

    /**
     *  These are all methods for manipulating and accessing screen position
     *  that derived classes have to implement
     * @return _more_
     */
    abstract String getPositionAttr();

    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public abstract double distance(int x, int y);

    /**
     * _more_
     *
     * @param x
     * @param y
     */
    public abstract void moveBy(int x, int y);

    /**
     * _more_
     *
     * @param x
     * @param y
     */
    public abstract void moveTo(int x, int y);

    /**
     * _more_
     * @return _more_
     */
    public abstract Rectangle getBounds();

    /**
     * _more_
     * @return _more_
     */
    public Rectangle getRepaintBounds() {
        return getBounds();
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public String getStretchPoint(int x, int y) {
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
        moveBy(x, y);
        return PT_CENTER;
    }




    /**
     *  Utility method for distance between two points
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return _more_
     */
    public static double distance(int x1, int y1, int x2, int y2) {
        return (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
    }


    /**
     * _more_
     *
     * @param canvas
     * @param x
     * @return _more_
     */
    public int transformOutputX(DisplayCanvas canvas, int x) {
        return canvas.transformOutputX(x);
    }

    /**
     * _more_
     *
     * @param canvas
     * @param x
     * @return _more_
     */
    public int transformOutputY(DisplayCanvas canvas, int x) {
        return canvas.transformOutputY(x);
    }


    /**
     * _more_
     *
     * @param canvas
     * @param r
     * @return _more_
     */
    public Rectangle transformOutput(DisplayCanvas canvas, Rectangle r) {
        return canvas.transformOutput(r);
    }


    /**
     * _more_
     *
     * @param canvas
     * @param r
     * @return _more_
     */
    public Point transformOutput(DisplayCanvas canvas, Point r) {
        return canvas.transformOutput(r);
    }





}

