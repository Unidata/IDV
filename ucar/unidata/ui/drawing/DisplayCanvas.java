/*
 * $Id: DisplayCanvas.java,v 1.17 2005/05/18 20:32:36 jeffmc Exp $
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
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import java.lang.reflect.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.event.*;

import javax.swing.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Hashtable;


/**
 * Class DisplayCanvas
 *
 *
 * @author IDV development team
 */
public class DisplayCanvas extends JPanel {


    /** _more_ */
    public static final String CMD_CREATE = "create";

    /** _more_ */
    public static final String CMD_TOFRONT = "tofront";

    /** _more_ */
    public static final String CMD_TOBACK = "toback";

    /** _more_ */
    public static final String CMD_CHANGE = "change";

    /** _more_ */
    public static final String CMD_REMOVE = "remove";

    /** _more_ */
    public static final String CMD_MOVE = "move";



    /** _more_ */
    public static final Cursor DEFAULT_CURSOR =
        new Cursor(Cursor.DEFAULT_CURSOR);

    /** _more_ */
    public static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);

    /** _more_ */
    public static final Cursor MOVE_CURSOR = new Cursor(Cursor.MOVE_CURSOR);

    /** _more_ */
    public static final Cursor TEXT_CURSOR = new Cursor(Cursor.TEXT_CURSOR);

    /** _more_ */
    public static final Cursor NW_CURSOR =
        new Cursor(Cursor.NW_RESIZE_CURSOR);


    public int gridSpacing = 20;

    public boolean showGrid = false;



    /** The scaling */
    protected double scaleFactor = 1.0;


    /** _more_ */
    protected Color canvasBg = Color.white;

    /** _more_ */
    protected List glyphs = new ArrayList();

    /** _more_ */
    List selectionSet = new ArrayList();

    /** _more_ */
    List cutBuffer;

    /** _more_ */
    protected Glyph highlightedGlyph = null;

    /** _more_ */
    int glyphCnt = 0;

    /** _more_ */
    private Component contents;


    /**
     * _more_
     *
     */
    public DisplayCanvas() {}


    /**
     * _more_
     *
     * @param c _more_
     */
    public void setCanvasBackground(Color c) {
        canvasBg = c;
        repaint();
    }

    /**
     * _more_
     * @return _more_
     */
    protected List getGlyphs() {
        return glyphs;
    }

    /**
     * _more_
     *
     * @param newGlyphs
     */
    protected void setGlyphs(List newGlyphs) {
        glyphs = newGlyphs;
        clearSelection();
        repaint();
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean isFocusTraversable() {
        return true;
    }



    /**
     * _more_
     * @return _more_
     */
    public Component getContents() {
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }

    public int snap(int p) {
        if(getShowGrid ()) {
            int dx =p%gridSpacing;
            p-=dx;
        }
        return p;
    }


    /**
       Set the ShowGrid property.

       @param value The new value for ShowGrid
    **/
    public void setShowGrid (boolean value) {
	this.showGrid = value;
        repaint();
    }

    /**
       Get the ShowGrid property.

       @return The ShowGrid
    **/
    public boolean getShowGrid () {
	return this.showGrid;
    }



    public void increaseGridSpacing() {
        gridSpacing+=2;
        repaint();
    }


    public void decreaseGridSpacing() {
        gridSpacing-=2;
        if(gridSpacing<2) gridSpacing = 2;
        repaint();
    }

    /**
     * _more_
     * @return _more_
     */
    protected Component doMakeContents() {
        return this;
    }

    public void paintGrid(Graphics g) {
        if(!showGrid) { return;}
        Graphics2D g2d = (Graphics2D)g;
        AffineTransform at = g2d.getTransform();
        double tx = at.getTranslateX();
        double  ty = at.getTranslateY();
        Rectangle b = getBounds();
        Stroke oldStroke =  g2d.getStroke();
        Stroke stroke = 
            new BasicStroke(1, 
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_BEVEL,
                            0,
                            new float[] {4,4}, 
                            0
                            );
        g2d.setStroke(stroke);
        g2d.setColor(Color.lightGray);
        //        g2d.translate(-tx,-ty);
        for(int i=0;i<b.width;i+=gridSpacing) {
            g.drawLine(i,0,i,b.height);
        }
        for(int i=0;i<b.height;i+=gridSpacing) {
            g.drawLine(0,i,b.width,i);
        }
        //        g2d.translate(tx,ty);
        g2d.setStroke(oldStroke);

    }


    /**
     * _more_
     *
     * @param g
     */
    public void paint(Graphics g) {
        //       super.paint(g);
        Rectangle clip = g.getClipBounds();
        if (clip != null) {
            clip.x      -= 2;
            clip.y      -= 2;
            clip.width  += 4;
            clip.height += 4;
        }
        if (clip != null) {
            clip = transformInput(clip);
        }
        clip = null;


        if (g instanceof Graphics2D) {
            ((Graphics2D) g).scale(scaleFactor, scaleFactor);
        }


        for (int i = 0; i < glyphs.size(); i++) {
            Glyph     glyph = (Glyph) glyphs.get(i);
            Rectangle gb    = glyph.getBounds();
            if ((clip == null) || clip.intersects(gb)) {
                glyph.paint(g, this);

            }
        }

        for (int i = 0; i < selectionSet.size(); i++) {
            Glyph     glyph = (Glyph) selectionSet.get(i);
            Rectangle gb    = glyph.getBounds();
            if ((clip == null) || clip.intersects(gb)) {
                glyph.paintSelection(g, this);
            }
        }
    }


    /**
     * _more_
     *
     * @param glyphClass
     * @param x
     * @param y
     * @return _more_
     */
    public Glyph createGlyph(String glyphClass, int x, int y) {
        Class c = null;

        try {
            c = Misc.findClass(glyphClass);
        } catch (Exception exc) {
            LogUtil.logException("Creating glyph:" + glyphClass, exc);
            return null;
        }
        Constructor ctor;
        Glyph       g = null;


        if (g == null) {
            try {
                ctor = c.getConstructor(new Class[]{ DisplayCanvas.class,
                                                     Integer.TYPE,
                                                     Integer.TYPE });
                if (ctor != null) {
                    g = (Glyph) ctor.newInstance(new Object[]{ this,
                                                               new Integer(x),
                                                               new Integer(
                                                               y) });
                }
            } catch (NoSuchMethodException nsme) {}
            catch (Exception exc) {
                LogUtil.logException("Creating: " + glyphClass, exc);
                return null;
            }
        }



        if (g == null) {
            try {
                ctor = c.getConstructor(new Class[]{ Integer.TYPE,
                                                     Integer.TYPE });
                if (ctor != null) {
                    g = (Glyph) ctor.newInstance(new Object[]{ new Integer(x),
                                                               new Integer(
                                                               y) });
                }
            } catch (NoSuchMethodException nsme) {}
            catch (Exception exc) {
                LogUtil.logException("Creating: " + glyphClass, exc);
                return null;
            }
        }

        if (g == null) {
            try {
                ctor = c.getConstructor(new Class[]{});
                if (ctor != null) {
                    g = (Glyph) ctor.newInstance(new Object[]{});
                }
            } catch (NoSuchMethodException nsme) {}
            catch (Exception exc) {
                LogUtil.logException("Creating: " + glyphClass, exc);
                return null;
            }
        }


        if (g == null) {
            throw new IllegalArgumentException("Unable to create glyph:"
                                               + glyphClass);
        }

        return g;
    }



    /**
     * _more_
     *
     * @param g
     */
    public void repaint(Glyph g) {
        super.repaint();
    }


    /**
     * _more_
     *
     * @param g
     */
    public void addGlyph(Glyph g) {
        glyphs.add(g);
        repaint(g);
    }


    /**
     * _more_
     *
     * @param r
     */
    public void select(Rectangle r) {
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph     glyph = (Glyph) glyphs.get(i);
            Rectangle gb    = glyph.getBounds();
            if (r.contains(gb.x, gb.y)
                    && r.contains(gb.x + gb.width, gb.y + gb.height)) {
                addSelection(glyph);
            }
        }
    }



    /**
     * _more_
     */
    public void selectionChanged() {}


    /**
     * _more_
     * @return _more_
     */
    public boolean hasSelection() {
        return (selectionSet.size() > 0);
    }

    /**
     * _more_
     *
     * @param g
     */
    public void addSelection(Glyph g) {
        if ( !isSelected(g)) {
            selectionSet.add(g);
            selectionChanged();
            repaint(g);
        }
    }

    /**
     * _more_
     *
     * @param g
     * @return _more_
     */
    public boolean isSelected(Glyph g) {
        return selectionSet.contains(g);
    }


    /**
     * _more_
     * @return _more_
     */
    public List getSelection() {
        return selectionSet;
    }

    /**
     * _more_
     */
    public void clearSelection() {
        selectionSet.clear();
        selectionChanged();
        repaint();
    }

    /**
     * _more_
     */
    public void clearAll() {
        glyphs.clear();
        highlightedGlyph = null;
        clearSelection();
    }

    /**
     * _more_
     *
     * @param theGlyph
     */
    public void notifyGlyphMoved(Glyph theGlyph) {}

    /**
     * _more_
     *
     * @param theGlyph
     */
    public void notifyGlyphMoveComplete(Glyph theGlyph) {}

    /**
     * _more_
     *
     * @param g
     * @param diddleSelection
     * @param fromPaste
     */
    public void notifyGlyphCreateComplete(Glyph g, boolean diddleSelection,
                                          boolean fromPaste) {}

    /**
     * _more_
     *
     * @param g
     */
    public void notifyGlyphCreateComplete(Glyph g) {
        notifyGlyphCreateComplete(g, true, false);
    }


    /**
     * _more_
     *
     * @param g
     */
    public void removeSelection(Glyph g) {
        selectionSet.remove(g);
        selectionChanged();
        repaint(g);
    }

    /**
     * _more_
     *
     * @param g
     */
    public void justRemoveGlyph(Glyph g) {
        doRemove(g);
    }

    /**
     * _more_
     *
     * @param g
     */
    public void removeGlyph(Glyph g) {
        doRemove(g);
    }

    /**
     * _more_
     *
     * @param g
     */
    public void doRemove(Glyph g) {
        if (highlightedGlyph == g) {
            repaint(highlightedGlyph);
            setHighlight(null);
        }
        removeSelection(g);
        glyphs.remove(g);
        if (g instanceof GroupGlyph) {
            List children =
                new ArrayList((List) ((GroupGlyph) g).getChildren());
            for (int i = 0; i < children.size(); i++) {
                doRemove((Glyph) children.get(i));
            }
        }
        g.doRemove();
        repaint(g);
    }


    /**
     * _more_
     *
     * @param g
     */
    public void setHighlight(Glyph g) {
        highlightedGlyph = g;
    }

    /**
     * _more_
     *
     * @param g
     */
    public void glyphChanged(Glyph g) {}


    /**
     * _more_
     *
     * @param path
     * @return _more_
     */
    public Image getImage(String path) {
        return GuiUtils.getImage(path, getClass());
    }


    /**
     * _more_
     *
     * @param id
     * @return _more_
     */
    public Glyph findGlyph(String id) {
        int num = glyphs.size();
        for (int i = 0; i < num; i++) {
            Glyph g = (Glyph) glyphs.get(i);
            if (g.getId().equals(id)) {
                return g;
            }
        }
        return null;
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public Glyph findGlyph(int x, int y) {
        return findGlyph(glyphs, x, y, 10.0);
    }

    /**
     * _more_
     *
     * @param glyphs
     * @param x
     * @param y
     * @param threshold
     * @return _more_
     */
    public static Glyph findGlyph(List glyphs, int x, int y,
                                  double threshold) {
        int    num         = glyphs.size();
        double minDistance = Double.MAX_VALUE;
        Glyph  minGlyph    = null;
        for (int i = 0; i < num; i++) {
            Glyph g = (Glyph) glyphs.get(i);
            if ( !g.pickable()) {
                continue;
            }
            double distance = g.distance(x, y);
            if ((distance < threshold) && (distance <= minDistance)) {
                minDistance = distance;
                minGlyph    = g;
            }
        }
        return minGlyph;
    }


    /**
     * _more_
     *
     * @param theGlyph
     */
    public void moveToFront(Glyph theGlyph) {
        glyphs.remove(theGlyph);
        glyphs.add(theGlyph);
        repaint(theGlyph);
    }



    /**
     * _more_
     *
     * @param theGlyph
     */
    public void moveToBack(Glyph theGlyph) {
        glyphs.remove(theGlyph);
        glyphs.add(0, theGlyph);
        repaint(theGlyph);
    }






    /**
     * _more_
     *
     * @param x
     * @return _more_
     */
    public int transformInputX(int x) {
        return x;
    }

    /**
     * _more_
     *
     * @param x
     * @return _more_
     */
    public int transformInputY(int x) {
        return x;
    }

    /**
     * _more_
     *
     * @param x
     * @return _more_
     */
    public int transformOutputX(int x) {
        return x;
    }

    /**
     * _more_
     *
     * @param x
     * @return _more_
     */
    public int transformOutputY(int x) {
        return x;
    }


    /**
     * _more_
     *
     * @param r
     * @return _more_
     */
    public Rectangle transformOutput(Rectangle r) {
        return r;
    }

    /**
     * _more_
     *
     * @param r
     * @return _more_
     */
    public Rectangle transformInput(Rectangle r) {
        return r;
    }

    /**
     * _more_
     *
     * @param r
     * @return _more_
     */
    public Point transformOutput(Point r) {
        return r;
    }

    /**
     * _more_
     *
     * @param r
     * @return _more_
     */
    public Point transformInput(Point r) {
        return r;
    }

}








