/*
 * $Id: PolyGlyphCreator.java,v 1.7 2005/05/13 18:32:10 jeffmc Exp $
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

import java.util.ArrayList;
import java.util.List;




/**
 * Class PolyGlyphCreator
 *
 *
 * @author IDV development team
 */
class PolyGlyphCreator extends CanvasCommand {

    /** _more_ */
    Glyph theGlyph;

    /** _more_ */
    int lastx;

    /** _more_ */
    int lasty;

    /**
     * _more_
     *
     * @param canvas
     * @param firstEvent
     * @param theGlyph
     * @param x
     * @param y
     *
     */
    public PolyGlyphCreator(DisplayCanvas canvas, AWTEvent firstEvent,
                            PolyGlyph theGlyph, int x, int y) {
        super(canvas, firstEvent, x, y);
        this.theGlyph = theGlyph;
        lastx         = x;
        lasty         = y;
    }

    /** _more_ */
    boolean shiftDown = false;


    /**
     * _more_
     * @return _more_
     */
    public Cursor getCursor() {
        return DisplayCanvas.HAND_CURSOR;
    }

    /**
     * _more_
     *
     * @param e
     * @return _more_
     */
    public CanvasCommand doKeyReleased(KeyEvent e) {
        shiftDown = false;
        return this;
    }


    /**
     * _more_
     *
     * @param e
     * @return _more_
     */
    public CanvasCommand doKeyPress(KeyEvent e) {
        shiftDown = (e.getKeyCode() == KeyEvent.VK_SHIFT);
        return this;
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
        if (theGlyph == null) {
            return null;
        }

        if (Glyph.distance(lastx, lasty, x, y) > 3.0) {
            //Math.abs(lastx-x)>2 || Math.abs(lasty-y)>2) {      

            lastx = x;
            lasty = y;
            ((PolyGlyph) theGlyph).addPoint(x, y);
            canvas.notifyGlyphMoved(theGlyph);
            canvas.repaint(theGlyph);
        }
        return this;
    }

    /**
     * _more_
     */
    protected void doComplete() {
        super.doComplete();
        //    canvas.notifyGlyphCreateComplete(theGlyph,true);
    }

}






