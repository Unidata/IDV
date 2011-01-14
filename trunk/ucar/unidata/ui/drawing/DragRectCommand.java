/*
 * $Id: DragRectCommand.java,v 1.9 2005/05/13 18:32:09 jeffmc Exp $
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
 *  This class draws a rectangle on its canvas as the mouse is dragged.
 *  When done dragging it tells the canvas to select the Glyphs in the
 *  rectangle
 */

class DragRectCommand extends CanvasCommand {

    /** _more_ */
    int anchorX;

    /** _more_ */
    int anchorY;

    /** _more_ */
    int currentX;

    /** _more_ */
    int currentY;

    /**
     * _more_
     *
     * @param canvas
     * @param firstEvent
     * @param x
     * @param y
     *
     */
    public DragRectCommand(EditCanvas canvas, AWTEvent firstEvent, int x,
                           int y) {
        super(canvas, firstEvent, x, y);
        anchorX  = x;
        anchorY  = y;
        currentX = x;
        currentY = y;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean doesChangeGlyphs() {
        return false;
    }


    /**
     *  Draw the currently dragged rectangle
     *
     * @param g
     */
    public void doPaint(Graphics g) {
        g.setColor(Color.lightGray);
        Rectangle r = canvas.transformOutput(getRect());
        g.drawRect(r.x, r.y, r.width, r.height);
    }

    /**
     *  Normalize the Rectangle
     * @return _more_
     */
    Rectangle getRect() {
        return new Rectangle(((anchorX < currentX)
                              ? anchorX
                              : currentX), ((anchorY < currentY)
                                            ? anchorY
                                            : currentY), Math.abs(anchorX
                                            - currentX), Math.abs(anchorY
                                                - currentY));
    }


    /**
     *  Reset the dragged x,y location and tell the canvas to repaint
     *  Return this to denote that the command is still active
     *
     * @param e
     * @param x
     * @param y
     * @return _more_
     */
    public CanvasCommand doMouseDragged(MouseEvent e, int x, int y) {
        currentX = x;
        currentY = y;
        canvas.repaint();
        return this;
    }


    /**
     *  Done
     *  Return null to denote that the command is not active
     *
     * @param e
     * @param x
     * @param y
     * @return _more_
     */
    public CanvasCommand doMouseReleased(MouseEvent e, int x, int y) {
        canvas.select(getRect());
        canvas.repaint();
        return null;
    }

    /**
     * _more_
     */
    protected void doComplete() {}


}  //DragRectCommand








