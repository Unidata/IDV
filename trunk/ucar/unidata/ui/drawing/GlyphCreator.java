/*
 * $Id: GlyphCreator.java,v 1.8 2005/05/13 18:32:09 jeffmc Exp $
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


/**
 * Class GlyphCreator
 *
 *
 * @author IDV development team
 */
class GlyphCreator extends GlyphStretcher {

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
    public GlyphCreator(DisplayCanvas canvas, AWTEvent firstEvent,
                        Glyph theGlyph, int x, int y) {
        super(canvas, firstEvent, theGlyph, null, x, y);
        canvas.repaint();
    }

    /**
     * _more_
     */
    protected void doComplete() {
        super.doComplete();
        canvas.repaint();
        theGlyph.setBeingCreated(false);
        canvas.notifyGlyphCreateComplete(theGlyph);

    }

}





