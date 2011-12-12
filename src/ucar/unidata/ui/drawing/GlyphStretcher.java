/*
 * $Id: GlyphStretcher.java,v 1.8 2005/05/13 18:32:10 jeffmc Exp $
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



import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Class GlyphStretcher
 *
 *
 * @author IDV development team
 */
class GlyphStretcher extends CanvasCommand {

    /** _more_ */
    Glyph theGlyph;

    /** _more_ */
    Point lastPoint;

    /** _more_ */
    String ptKey = "";

    /** _more_ */
    List selection;

    /**
     * _more_
     *
     * @param canvas
     * @param firstEvent
     * @param theGlyph
     * @param selection
     * @param x
     * @param y
     *
     */
    public GlyphStretcher(DisplayCanvas canvas, AWTEvent firstEvent,
                          Glyph theGlyph, List selection, int x, int y) {
        super(canvas, firstEvent, x, y);
        this.theGlyph  = theGlyph;
        this.selection = selection;
        if ((selection != null) && (selection.size() > 1)) {
            ptKey = Glyph.PT_CENTER;
        } else {
            ptKey = theGlyph.getStretchPoint(x, y);
        }
        lastPoint = new Point(x, y);
        doMouseDragged(null, x, y);
    }

    /**
     * _more_
     * @return _more_
     */
    public Cursor getCursor() {
        return DisplayCanvas.MOVE_CURSOR;
    }

    /**
     * _more_
     * @return _more_
     */
    public String getDescription() {
        return "Resize a glyph";
    }

    /**
     * _more_
     *
     * @param e
     * @param x
     * @param y
     * @return _more_
     */
    public CanvasCommand doMouseDragged(MouseEvent e, int x, int y) {
        x = canvas.snap(x);
        y = canvas.snap(y);

        if (theGlyph == null) {
            return null;
        }
        canvas.repaint(theGlyph);
        int     destx    = x;
        int     desty    = y;
        boolean absolute = true;

        if (ptKey.equals(Glyph.PT_CENTER)) {
            destx    = x - lastPoint.x;
            desty    = y - lastPoint.y;
            absolute = false;
        }
        if (selection != null) {
            for (int i = 0; i < selection.size(); i++) {
                Glyph g = (Glyph) selection.get(i);
                if (g != theGlyph) {
                    canvas.repaint(g);
                    if (g.canStretch()) {
                        g.stretchTo(destx, desty, ptKey, true);
                    } else {
                        //          if (absolute) g.moveTo(destx,desty);    else      g.moveBy(destx,desty);                  
                    }
                    canvas.repaint(g);
                }
            }
        }

        if (theGlyph.canStretch()) {
            ptKey = theGlyph.stretchTo(destx, desty, ptKey, true);
        } else {
            if (absolute) {
                theGlyph.moveTo(destx, desty);
            } else {
                theGlyph.moveBy(destx, desty);
            }
        }

        canvas.repaint(theGlyph);
        lastPoint.x = x;
        lastPoint.y = y;
        canvas.notifyGlyphMoved(theGlyph);
        return this;
    }


    /**
     * _more_
     */
    protected void doComplete() {
        super.doComplete();
        canvas.notifyGlyphMoveComplete(theGlyph);
    }
}





