/*
 * $Id: CanvasCommand.java,v 1.9 2005/05/13 18:32:09 jeffmc Exp $
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
 * Class CanvasCommand
 *
 *
 * @author IDV development team
 */
public class CanvasCommand {

    /** _more_ */
    public DisplayCanvas canvas;

    /** _more_ */
    public AWTEvent firstEvent;

    /** _more_ */
    public Point originalPoint;

    /** _more_ */
    public static boolean debug = false;


    /**
     * _more_
     *
     * @param canvas
     * @param firstEvent
     * @param x
     * @param y
     *
     */
    public CanvasCommand(DisplayCanvas canvas, AWTEvent firstEvent, int x,
                         int y) {
        this.canvas        = canvas;
        this.firstEvent    = firstEvent;
        this.originalPoint = new Point(x, y);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean doesChangeGlyphs() {
        return true;
    }

    /**
     * _more_
     *
     * @param msg
     */
    public void debug(String msg) {
        if (debug) {
            System.err.println(msg);
        }
    }

    /**
     * _more_
     * @return _more_
     */
    public String getDescription() {
        return " ";
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean isAtomic() {
        return false;
    }

    /**
     * _more_
     */
    public void doAbort() {}

    /**
     * _more_
     */
    protected void doComplete() {}

    /**
     * _more_
     * @return _more_
     */
    public Cursor getCursor() {
        return null;
    }


    /**
     * _more_
     *
     * @param e
     * @return _more_
     */
    public CanvasCommand doFocusGained(FocusEvent e) {
        debug("CanvasCommand.doFocusGained");
        //    doComplete ();
        return null;
    }

    /**
     * _more_
     *
     * @param e
     * @return _more_
     */
    public CanvasCommand doFocusLost(FocusEvent e) {
        debug("CanvasCommand.doFocusLost");
        //    doComplete ();
        return null;
    }



    /**
     * _more_
     *
     * @param graphics
     */
    public void doPaint(Graphics graphics) {}


    /**
     * _more_
     *
     * @param e
     * @param x
     * @param y
     * @return _more_
     */
    public CanvasCommand doMouseClicked(MouseEvent e, int x, int y) {
        debug("CanvasCommand.doMouseClicked");
        //    doComplete ();
        return null;
    }


    /**
     * _more_
     *
     * @param e
     * @param x
     * @param y
     * @return _more_
     */
    public CanvasCommand doMousePressed(MouseEvent e, int x, int y) {
        debug("CanvasCommand.doMousePressed");
        //    doComplete ();
        return null;
    }


    /**
     * _more_
     *
     * @param e
     * @param x
     * @param y
     * @return _more_
     */
    public CanvasCommand doMouseReleased(MouseEvent e, int x, int y) {
        debug("CanvasCommand.doMouseReleased");
        return null;
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
        debug("CanvasCommand.doMouseDragged");
        //    doComplete();
        return null;
    }

    /**
     * _more_
     *
     * @param e
     * @return _more_
     */
    public CanvasCommand doKeyReleased(KeyEvent e) {
        return this;
    }

    /**
     * _more_
     *
     * @param e
     * @return _more_
     */
    public CanvasCommand doKeyPress(KeyEvent e) {
        debug("CanvasCommand.doKeyPress");
        //    doComplete ();
        return null;
    }


    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return getDescription();
    }

    /**
     * _more_
     *
     * @param g
     */
    public void paint(Graphics g) {}

    /**
     * _more_
     */
    public void repaint() {
        canvas.repaint();
    }


    /**
     * _more_
     *
     * @param r
     */
    public void repaint(Rectangle r) {
        canvas.repaint(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
    }

}





