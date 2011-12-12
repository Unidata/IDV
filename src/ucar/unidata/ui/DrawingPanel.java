/*
 * $Id: DrawingPanel.java,v 1.9 2007/07/06 20:45:30 jeffmc Exp $
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

package ucar.unidata.ui;


import ucar.unidata.ui.event.UIChangeEvent;
import ucar.unidata.ui.event.UIChangeListener;



import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.util.*;

import javax.swing.*;


/**
 * Utility class for allowing users to select, resize, and move "Drawables" on a JPanel.
 * <br>
 * UIChangeEvent is thrown when a Drawable is selected, deselected, resized or moved. <pre>
 *   events can be:  property                   newValue
 *                  DrawingPanel.SELECT        Boolean   (true: selected, false: deselected)
 *                  DrawingPanel.RESIZE        Dimension  (new size)
 *                  DrawingPanel.MOVE          ??         (new position)
 * </pre>
 * @see ucar.unidata.ui.Drawable
 *
 * @author John Caron
 * @version $Id: DrawingPanel.java,v 1.9 2007/07/06 20:45:30 jeffmc Exp $
 */

public class DrawingPanel extends JPanel {

    /** _more_ */
    public static final String SELECT = "select";

    /** _more_ */
    public static final String RESIZE = "resize";

    /** _more_ */
    public static final String MOVE = "move";

    /** _more_ */
    private Vector comps = new Vector();

    /** _more_ */
    private Drawable currC = null;  // current = selected

    /** _more_ */
    private boolean xorMode = false;

    /** _more_ */
    private boolean wasDragged = false;

    /** _more_ */
    private boolean resizingMode = false;

    /** _more_ */
    private boolean useCenterPoint = true;

    /** _more_ */
    private int startx, starty;

    /** _more_ */
    private ucar.unidata.util.ListenerManager lm;

    /** _more_ */
    private Point2D.Double centerPoint = new Point2D.Double(0.0, 0.0);

    /** _more_ */
    private boolean debugPick = false;

    /**
     * constructor
     * @param changeable true if the user can edit/change
     */
    public DrawingPanel(boolean changeable) {
        lm = new ucar.unidata.util.ListenerManager(
            "ucar.unidata.ui.event.UIChangeListener",
            "ucar.unidata.ui.event.UIChangeEvent", "processChange");

        if (changeable) {
            this.addMouseListener(new MyMouseListener());
            this.addMouseMotionListener(new MyMouseMotionListener());
        }

        setBackground(Color.white);
        setPreferredSize(new Dimension(200, 200));

        // track center of the component
        setLayout(new BorderLayout() {  // override BorderLayout so as to center the symbol
            public void layoutContainer(Container parent) {
                super.layoutContainer(parent);
                Rectangle r = getBounds();
                centerPoint.x = r.width / 2;
                centerPoint.y = r.height / 2;
            }
        });
    }

    // event management

    /**
     * _more_
     *
     * @param l
     */
    public void addUIChangeListener(UIChangeListener l) {
        lm.addListener(l);
    }

    /**
     * _more_
     *
     * @param l
     */
    public void removeUIChangeListener(UIChangeListener l) {
        lm.removeListener(l);
    }

    /**
     * Get the center of the DrawingPanel
     * @return the center of the DrawingPanel
     */
    public Point2D getCenterPoint() {
        return centerPoint;
    }

    /**
     * May optionally keep the Drawables reletive to the center of the DrawingPanel
     * @param use true if the Drawables should stay centerd in the DrawingPanel
     */
    public void useCenterPoint(boolean use) {
        this.useCenterPoint = use;
    }

    /**
     * Add a Drawable to the top. If Drawable bounds overlap, the topmost
     *  is found when picking/selecting with the mouse.
     *
     * @param d
     */
    public void addDrawable(Drawable d) {
        comps.addElement(d);
    }

    /** remove all Drawables */
    public void removeAllDrawables() {
        comps = new Vector();
    }

    /**
     * paint the DrawingPanel
     *
     * @param g
     */
    public void paintComponent(Graphics g) {
        //g.setPaintMode();
        super.paintComponent(g);  // Fill the background

        // LOOK : this needs to be optional
        Rectangle b = getBounds();
        g.setColor(Color.lightGray);
        g.drawLine(b.width / 2, 0, b.width / 2, b.height);
        g.drawLine(0, b.height / 2, b.width, b.height / 2);

        // loop through all components
        for (int i = 0; i < comps.size(); i++) {
            Drawable d = (Drawable) comps.elementAt(i);
            if ( !d.isActive()) {
                continue;
            }

            d.draw((Graphics2D) g, centerPoint);

            if (d.isSelected() && resizingMode) {
                Rectangle r = getCompBounds(d);
                g.drawRect(r.x, r.y, r.width, r.height);
            }
        }
    }

    /**
     * Select a Drawable by name.
     * @param name    unique name of Drawable.
     */
    public void select(String name) {
        if (null == name) {
            select((Drawable) null);  // deselect
            return;
        }

        for (int i = 0; i < comps.size(); i++) {
            Drawable d = (Drawable) comps.elementAt(i);
            if (0 == name.compareTo(d.getName())) {
                select(d);
            }
        }
        return;
    }

    ////////////////////// private methods

    /**
     * Make this Drawable the selected one. Deselect the current Drawable if
     *  there is one.
     *  @param d    select this Drawable, or null to deselect current Drawable.
     */
    private void select(Drawable d) {
        if (currC == d) {
            return;
        }

        if (currC != null) {  // turn off currently selected
            currC.setSelected(false);
            lm.sendEvent(new UIChangeEvent(this, SELECT, currC,
                                           new Boolean(false)));
            currC = null;
        }
        if (d != null) {
            currC = d;
            currC.setSelected(true);
            lm.sendEvent(new UIChangeEvent(this, SELECT, currC,
                                           new Boolean(true)));
            resizingMode = false;
            wasDragged   = false;
        }
        repaint();
    }

    // return picked Drawable, else null

    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    private Drawable pick(int x, int y) {
        if (debugPick) {
            System.out.println("Search for " + x + " " + y);
        }

        // search all components, starting with "top"
        for (int i = comps.size() - 1; i >= 0; i--) {
            Drawable d = (Drawable) comps.elementAt(i);
            if ( !d.isActive()) {
                continue;
            }

            Rectangle r = getCompBounds(d);
            if (debugPick) {
                System.out.println("    bounds " + r);
            }
            if (r.contains(x, y)) {
                return d;
            }
        }
        return null;
    }

    /**
     * _more_
     *
     * @param d
     * @return _more_
     */
    private Rectangle getCompBounds(Drawable d) {
        Rectangle r = d.getBounds();
        if (useCenterPoint) {
            return new Rectangle((int) (r.x + centerPoint.x),
                                 (int) (r.y + centerPoint.y), r.width,
                                 r.height);
        } else {
            return r;
        }
    }

    // current = selected ?

    /**
     * _more_
     */
    private void drawCurrent() {
        if (currC == null) {
            return;
        }

        Graphics g = getGraphics();
        if (xorMode) {
            g.setXORMode(getBackground());
        } else {
            g.setPaintMode();
        }
        currC.draw((Graphics2D) g, centerPoint);

        if (resizingMode) {
            Rectangle r = getCompBounds(currC);
            g.drawRect(r.x, r.y, r.width, r.height);
        }
        g.dispose();
    }

    // MouseListener interface

    /**
     * Class MyMouseListener
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private class MyMouseListener extends MouseAdapter {

        // assumes mouseClicked called after mousePressed

        /**
         * _more_
         *
         * @param evt
         */
        public void mouseClicked(MouseEvent evt) {
            if (SwingUtilities.isLeftMouseButton(evt)) {
                // select component, put in resize mode
                if (null != pick(evt.getX(), evt.getY())) {
                    resizingMode = (evt.getClickCount() > 1);
                }
                repaint();
            }
        }

        /**
         * _more_
         *
         * @param evt
         */
        public void mousePressed(MouseEvent evt) {
            if (SwingUtilities.isLeftMouseButton(evt)) {
                startx = evt.getX();
                starty = evt.getY();
                // select component, put in move mode
                Drawable d = pick(startx, starty);
                select(d);  // if null, deselects
                drawCurrent();
            }
        }

        /**
         * _more_
         *
         * @param evt
         */
        public void mouseReleased(MouseEvent evt) {
            //System.out.println("mouseReleased "+evt.getClickCount());
            if (SwingUtilities.isLeftMouseButton(evt)) {
                if (currC == null) {
                    return;
                }

                xorMode = false;
                if (wasDragged) {
                    if (resizingMode) {
                        int width  = Math.abs(startx - evt.getX());
                        int height = Math.abs(starty - evt.getY());
                        currC.setSize(width, height);
                        lm.sendEvent(new UIChangeEvent(this, RESIZE, currC,
                                new Dimension(width, height)));
                    } else {
                        // LOOK: constrain inside the panel? (int) (x - centerPoint.x), (int) (y - centerPoint.y)
                        int x = useCenterPoint
                                ? (int) (evt.getX() - centerPoint.x)
                                : evt.getX();
                        int y = useCenterPoint
                                ? (int) (evt.getY() - centerPoint.y)
                                : evt.getY();
                        currC.setPosition(x, y);
                        lm.sendEvent(new UIChangeEvent(this, MOVE, currC,
                                evt.getPoint()));
                    }

                    setCursor(
                        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    repaint();
                    wasDragged = false;
                }

            }
        }
    }  // end myMouseListener

    // MouseMotionListener interface

    /**
     * Class MyMouseMotionListener
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private class MyMouseMotionListener extends MouseMotionAdapter {

        /**
         * _more_
         *
         * @param evt
         */
        public void mouseDragged(MouseEvent evt) {
            if (currC == null) {
                return;
            }

            if (wasDragged) {
                drawCurrent();  // Remove shape
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                xorMode = true;
            }

            if (resizingMode) {
                int width  = Math.abs(startx - evt.getX());
                int height = Math.abs(starty - evt.getY());
                currC.setSize(width, height);
            } else {
                // LOOK: constrain inside the panel?
                int x = useCenterPoint
                        ? (int) (evt.getX() - centerPoint.x)
                        : evt.getX();
                int y = useCenterPoint
                        ? (int) (evt.getY() - centerPoint.y)
                        : evt.getY();
                currC.setPosition(x, y);
            }
            drawCurrent();  // Redraw it
            wasDragged = true;
        }
    }  // end MyMouseMotionListener

}

/*
 *  Change History:
 *  $Log: DrawingPanel.java,v $
 *  Revision 1.9  2007/07/06 20:45:30  jeffmc
 *  A big J&J
 *
 *  Revision 1.8  2005/05/13 18:31:45  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.7  2004/09/07 18:36:21  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.6  2004/02/27 21:19:17  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.5  2004/01/29 17:37:09  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.4  2000/08/18 04:15:53  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.3  1999/06/03 01:44:10  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:05  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:48  caron
 *  startAgain
 *
 * # Revision 1.6  1999/03/26  19:58:17  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.5  1999/03/16  17:00:30  caron
 * # fix StationModel editing; add TopLevel
 * #
 * # Revision 1.4  1999/02/15  23:06:39  caron
 * # upgrade to java2D, new ProjectionManager
 * #
 * # Revision 1.3  1998/12/14  17:11:58  russ
 * # Add comment for accumulating change histories.
 * #
 */






