/*
 * $Id: CompositeGlyph.java,v 1.9 2005/05/13 18:32:09 jeffmc Exp $
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



import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;

import java.awt.Point;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;


/**
 * Class CompositeGlyph
 *
 *
 * @author IDV development team
 */
public abstract class CompositeGlyph extends RectangleGlyph {

    /** _more_ */
    List children = new ArrayList();

    /** _more_ */
    boolean paintBorder = true;

    /** _more_ */
    boolean strongOwnership = false;


    /**
     * _more_
     *
     * @param type
     * @param newChildren
     *
     */
    protected CompositeGlyph(String type, List newChildren) {
        this(type, 0, 0, newChildren);
    }

    /**
     * _more_
     *
     * @param type
     * @param x
     * @param y
     *
     */
    protected CompositeGlyph(String type, int x, int y) {
        this(type, x, y, null);
    }

    /**
     * _more_
     *
     * @param type
     *
     */
    protected CompositeGlyph(String type) {
        this(type, 0, 0, null);
    }

    /**
     * _more_
     *
     * @param type
     * @param x
     * @param y
     * @param newChildren
     *
     */
    protected CompositeGlyph(String type, int x, int y, List newChildren) {
        super(type, x, y, 1, 1);
        if (newChildren != null) {
            for (int i = 0; i < newChildren.size(); i++) {
                addChild((Glyph) newChildren.get(i));
            }
        }
    }

    /**
     * _more_
     *
     * @param g
     */
    public void childChanged(Glyph g) {
        calculateBounds();
    }

    /**
     * _more_
     *
     * @param g
     */
    public void removeChild(Glyph g) {
        children.remove(g);
        calculateBounds();
    }


    /**
     * _more_
     * @return _more_
     */
    public List getChildren() {
        return children;
    }

    /**
     * _more_
     */
    public void unGroup() {
        for (int i = 0; i < children.size(); i++) {
            Glyph child = (Glyph) children.get(i);
            if (strongOwnership) {
                child.doRemove();
            }
            child.setParent(null);
        }
        children.clear();
    }



    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public double distance(int x, int y) {
        if ( !getBoundsFromChildren() || (children.size() == 0)) {
            return super.distance(x, y);
        }
        double min = (paintBorder
                      ? super.distance(x, y)
                      : Double.MAX_VALUE);
        for (int i = 0; i < children.size(); i++) {
            Glyph child = (Glyph) children.get(i);
            min = Math.min(child.distance(x, y), min);
        }
        return min;
    }


    /**
     * _more_
     *
     * @param g
     */
    public void addChild(Glyph g) {
        addChild(g, true);
    }

    /**
     * _more_
     *
     * @param g
     * @param calculateBounds
     */
    public void addChild(Glyph g, boolean calculateBounds) {
        children.add(g);
        g.setParent(this);
        if (calculateBounds) {
            calculateBounds();
        }
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean getBoundsFromChildren() {
        return true;
    }

    /**
     * _more_
     */
    public void calculateBounds() {
        if ( !getBoundsFromChildren() || (children.size() == 0)) {
            return;
        }
        bounds = null;
        for (int i = 0; i < children.size(); i++) {
            Glyph     child = (Glyph) children.get(i);
            Rectangle cb    = child.getBounds();
            if (bounds == null) {
                bounds = new Rectangle(cb);
            } else {
                bounds.add(cb);
            }
        }
        if (bounds == null) {
            bounds = new Rectangle(0, 0, 0, 0);
        } else {
            bounds.x      -= 2;
            bounds.y      -= 2;
            bounds.width  += 4;
            bounds.height += 4;
        }
    }



    /**
     * _more_
     * @return _more_
     *
     * @throws CloneNotSupportedException
     */
    protected Object clone() throws CloneNotSupportedException {
        CompositeGlyph clonedObject = (CompositeGlyph) super.clone();
        return clonedObject;
    }


    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paint(Graphics g, DisplayCanvas c) {
        paintChildren(g, c);
    }

    /**
     * _more_
     *
     * @param g
     * @param c
     */
    protected void paintChildren(Graphics g, DisplayCanvas c) {
        if (strongOwnership) {
            for (int i = 0; i < children.size(); i++) {
                Glyph child = (Glyph) children.get(i);
                child.paint(g, c);
            }
        }
        if (paintBorder) {
            g.setColor(Color.lightGray);
            Rectangle r = bounds;
            g.drawRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
        }
    }

    /**
     * _more_
     *
     * @param c
     */
    public void setForeground(Color c) {
        super.setForeground(c);
        for (int i = 0; i < children.size(); i++) {
            Glyph child = (Glyph) children.get(i);
            child.setForeground(c);
        }
    }


    /**
     * _more_
     *
     * @param c
     */
    public void setBackground(Color c) {
        super.setBackground(c);
        for (int i = 0; i < children.size(); i++) {
            Glyph child = (Glyph) children.get(i);
            child.setBackground(c);
        }
    }


    /**
     * _more_
     *
     * @param c
     */
    public void setLineWidth(int c) {
        super.setLineWidth(c);
        for (int i = 0; i < children.size(); i++) {
            Glyph child = (Glyph) children.get(i);
            child.setLineWidth(c);
        }
        calculateBounds();
    }

    /**
     * _more_
     *
     * @param c
     */
    public void setFilled(boolean c) {
        super.setFilled(c);
        for (int i = 0; i < children.size(); i++) {
            Glyph child = (Glyph) children.get(i);
            child.setFilled(c);
        }
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
        super.stretchTo(x, y, pt, correct);
        for (int i = 0; i < children.size(); i++) {
            Glyph child = (Glyph) children.get(i);
            child.stretchTo(x, y, pt, correct);
        }
        calculateBounds();
        return pt;
    }


    /**
     * _more_
     *
     * @param x
     * @param y
     */
    public void moveTo(int x, int y) {
        Point delta = new Point(x - bounds.x, y - bounds.y);
        bounds.x = x;
        bounds.y = y;
        for (int i = 0; i < children.size(); i++) {
            Glyph child = (Glyph) children.get(i);
            child.moveBy(delta.x, delta.y);
        }
        calculateBounds();
    }

    /**
     * _more_
     * @return _more_
     */
    public String getAttrs() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < children.size(); i++) {
            Glyph child = (Glyph) children.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append(child.getId());
        }
        return makeAttr(ATTR_CHILDREN, sb.toString()) + super.getAttrs();

    }


    /**
     * _more_
     *
     * @param name
     * @param value
     */
    public void setAttr(String name, String value) {
        if (ATTR_CHILDREN.equals(name)) {
            StringTokenizer st = new StringTokenizer(value, ",");
            for (int i = 0; i < children.size(); i++) {
                Glyph child = (Glyph) children.get(i);
                child.setParent(null);
            }
            children.clear();
            while (st.hasMoreTokens()) {
                String cId   = st.nextToken();
                Glyph  child = (Glyph) idToGlyph.get(cId);
                if (child != null) {
                    addChild(child, false);
                }
            }
            calculateBounds();
        } else {
            super.setAttr(name, value);
        }

    }



}





