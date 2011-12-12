/*
 * $Id: Rubberband.java,v 1.9 2007/07/06 20:45:33 jeffmc Exp $
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



import java.awt.*;
import java.awt.event.*;


/**
 * Implements XOR rubberbanding.
 * @author David M. Geary
 * @author John Caron
 * @version $Id: Rubberband.java,v 1.9 2007/07/06 20:45:33 jeffmc Exp $
 */
abstract public class Rubberband {

    /** _more_ */
    protected Point anchorPt = new Point(0, 0);

    /** _more_ */
    protected Point stretchedPt = new Point(0, 0);

    /** _more_ */
    protected Point lastPt = new Point(0, 0);

    /** _more_ */
    protected Point endPt = new Point(0, 0);

    /** _more_ */
    private Component component;

    /** _more_ */
    private boolean firstStretch = true;

    /** _more_ */
    private boolean active = false;

    /**
     * _more_
     *
     * @param g
     */
    abstract public void drawLast(Graphics g);

    /**
     * _more_
     *
     * @param g
     */
    abstract public void drawNext(Graphics g);

    /**
     * _more_
     *
     */
    public Rubberband() {}

    /**
     * _more_
     *
     * @param c
     *
     */
    public Rubberband(Component c) {
        setComponent(c);
    }

    /**
     * _more_
     *
     * @param b
     */
    public void setActive(boolean b) {
        active = b;
    }

    /**
     * _more_
     *
     * @param c
     */
    public void setComponent(Component c) {
        component = c;
        /* component.addMouseListener(new MouseAdapter() {
           public void mousePressed(MouseEvent event) {
              if(isActive()) {
                 anchor(event.getPoint());
              }
           }
           public void mouseClicked(MouseEvent event) {
              if(isActive())
                 end(event.getPoint());
           }
           public void mouseReleased(MouseEvent event) {
              if(isActive())
                 end(event.getPoint());
           }
        }); */
        component.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent event) {
                if (isActive()) {
                    stretch(event.getPoint());
                }
            }
        });
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean isActive() {
        return active;
    }

    /**
     * _more_
     * @return _more_
     */
    public Point getAnchor() {
        return anchorPt;
    }

    /**
     * _more_
     * @return _more_
     */
    public Point getStretched() {
        return stretchedPt;
    }

    /**
     * _more_
     * @return _more_
     */
    public Point getLast() {
        return lastPt;
    }

    /**
     * _more_
     * @return _more_
     */
    public Point getEnd() {
        return endPt;
    }

    /**
     * _more_
     *
     * @param p
     */
    public void anchor(Point p) {
        firstStretch  = true;
        anchorPt.x    = p.x;
        anchorPt.y    = p.y;

        stretchedPt.x = lastPt.x = anchorPt.x;
        stretchedPt.y = lastPt.y = anchorPt.y;
    }

    /**
     * _more_
     *
     * @param p
     */
    public void stretch(Point p) {
        lastPt.x      = stretchedPt.x;
        lastPt.y      = stretchedPt.y;
        stretchedPt.x = p.x;
        stretchedPt.y = p.y;

        Graphics g = component.getGraphics();
        if (g != null) {
            try {
                g.setXORMode(component.getBackground());
                if (firstStretch == true) {
                    firstStretch = false;
                } else {
                    drawLast(g);
                }
                drawNext(g);
            } finally {
                g.dispose();
            }  // try
        }      // if
    }

    /**
     * _more_
     *
     * @param p
     */
    public void end(Point p) {
        lastPt.x = endPt.x = p.x;
        lastPt.y = endPt.y = p.y;

        Graphics g = component.getGraphics();
        if (g != null) {
            try {
                g.setXORMode(component.getBackground());
                drawLast(g);
            } finally {
                g.dispose();
            }
        }
    }

    /**
     * _more_
     * @return _more_
     */
    public Rectangle getBounds() {
        return new Rectangle((stretchedPt.x < anchorPt.x)
                             ? stretchedPt.x
                             : anchorPt.x, (stretchedPt.y < anchorPt.y)
                                           ? stretchedPt.y
                                           : anchorPt.y, Math.abs(
                                           stretchedPt.x
                                           - anchorPt.x), Math.abs(
                                               stretchedPt.y - anchorPt.y));
    }

    /**
     * _more_
     * @return _more_
     */
    public Rectangle lastBounds() {
        return new Rectangle((lastPt.x < anchorPt.x)
                             ? lastPt.x
                             : anchorPt.x, (lastPt.y < anchorPt.y)
                                           ? lastPt.y
                                           : anchorPt.y, Math.abs(lastPt.x
                                           - anchorPt.x), Math.abs(lastPt.y
                                               - anchorPt.y));
    }
}

/*
 *  Change History:
 *  $Log: Rubberband.java,v $
 *  Revision 1.9  2007/07/06 20:45:33  jeffmc
 *  A big J&J
 *
 *  Revision 1.8  2005/05/13 18:31:51  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.7  2004/09/07 18:36:26  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.6  2004/02/27 21:19:20  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.5  2004/01/29 17:37:12  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.4  2000/08/18 04:15:57  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.3  1999/06/03 01:44:13  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:08  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:48  caron
 *  startAgain
 *
 * # Revision 1.3  1999/03/26  19:58:33  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.2  1998/12/14  17:11:59  russ
 * # Add comment for accumulating change histories.
 * #
 */






