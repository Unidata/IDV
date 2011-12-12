/*
 * $Id: Navigation.java,v 1.29 2006/04/04 21:41:19 jeffmc Exp $
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

package ucar.unidata.view.geoloc;



import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.ListenerManager;


/**
 * Consider this a private inner class of NavigatedPanel.
 *   Handle display to world coordinate transformation, always linear.
 *   throw NewMapAreaEvent when MapArea changes
 *   allow setMapArea() to be called; this will also trigger a NewMapAreaEvent
 *
 * @author John Caron
 * @version $Id: Navigation.java,v 1.29 2006/04/04 21:41:19 jeffmc Exp $
 */

public class Navigation {

    // fundamental quantities

    /** current display size */
    private double pwidth  = 0,
                   pheight = 0;

    /** scale the same in both directions */
    private double pix_per_world = 1.0;

    /** Initial scale */
    private double initialScale = 1.0;



    /** offset from world origin in pixels */
    private double pix_x0 = 0.0,
                   pix_y0 = 0.0;

    // derived

    /** current world bounding box */
    private ProjectionRect bb;

    /** affine transform for graphics2D */
    private AffineTransform at;
    //misc

    /** can't initialize until screen size is known */
    private boolean mapAreaIsSet = false;

    /** can't initialize until initial bounding box is known */
    private boolean screenSizeIsSet = false;

    /** zoom stack */
    private ZoomStack zoom = new ZoomStack();

    /** manage NewMapAreaListener's */
    private ListenerManager lm;

    /** debug flags */
    private static boolean
        debug          = false,
        debugZoom      = false,
        debugTransform = false;

    /** more debug flags */
    private static boolean
        debugRecalc  = false,
        debugMapArea = false;

    /**
     * Create a new navigation.
     */
    public Navigation() {
        bb = new ProjectionRect();
        at = new AffineTransform();

        // manage NewMapAreaListener's
        lm = new ucar.unidata.util.ListenerManager(
            "ucar.unidata.view.geoloc.NewMapAreaListener",
            "ucar.unidata.view.geoloc.NewMapAreaEvent", "actionPerformed");
    }

    // event listener managagment

    /**
     * Add a {@link NewMapAreaListener}.
     *
     * @param l  listener to add
     */
    public void addNewMapAreaListener(NewMapAreaListener l) {
        lm.addListener(l);
    }

    /**
     * Remove a {@link NewMapAreaListener}.
     *
     * @param l  listener to remove
     */
    public void removeNewMapAreaListener(NewMapAreaListener l) {
        lm.removeListener(l);
    }

    // screen size

    /**
     * Get the screen width
     * @return screen width (pixels)
     */
    public double getScreenWidth() {
        return pwidth;
    }

    /**
     * Get the screen height
     * @return screen height (pixels)
     */
    public double getScreenHeight() {
        return pheight;
    }

    /**
     * Set the screen size
     *
     * @param pwidth     width (pixels)
     * @param pheight    height (pixels)
     */
    public void setScreenSize(double pwidth, double pheight) {
        if ((pwidth == 0) || (pheight == 0)) {
            return;
        }

        this.pwidth  = pwidth;
        this.pheight = pheight;
        if (mapAreaIsSet && !screenSizeIsSet) {  // only have to do this the first time
            recalcFromBoundingBox();
        }
        screenSizeIsSet = true;
        if (initialScale != 1.0) {
            zoom(initialScale);
            initialScale = 1.0;
        }

        fireMapAreaEvent();
    }

    /**
     * Get the affine transform based on screen size and world bounding box
     * @return the transform
     */
    public AffineTransform getTransform() {
        at.setTransform(pix_per_world, 0.0, 0.0, -pix_per_world, pix_x0,
                        pix_y0);

        if (debug) {
            System.out.println("Navigation getTransform = " + pix_per_world
                               + " " + pix_x0 + " " + pix_y0);
            System.out.println("  transform = " + at);
        }
        return at;
    }


    /**
     * Calculate if we want to rotate based on aspect ratio
     *
     * @param displayWidth     width of display
     * @param displayHeight    height of display
     * @return true if aspects are different
     */
    public boolean wantRotate(double displayWidth, double displayHeight) {
        getMapArea(bb);  // current world bounding box
        boolean aspectDisplay = displayHeight < displayWidth;
        boolean aspectWorldBB = bb.getHeight() < bb.getWidth();
        return (aspectDisplay ^ aspectWorldBB);  // aspects are different
    }

    /**
     * Calculate an affine transform based on the display size parameters
     * - used for printing.
     * @param rotate should the page be rotated
     * @param displayX       upper right corner X
     * @param displayY       upper right corner Y
     * @param displayWidth   display width
     * @param displayHeight  display height
     * @return transform
     */
    public AffineTransform calcTransform(boolean rotate, double displayX,
                                         double displayY,
                                         double displayWidth,
                                         double displayHeight) {
        getMapArea(bb);  // current world bounding box
        // scale to limiting dimension
        double pxpsx, pypsy;
        if (rotate) {
            pxpsx = displayHeight / bb.getWidth();
            pypsy = displayWidth / bb.getHeight();
        } else {
            pxpsx = displayWidth / bb.getWidth();
            pypsy = displayHeight / bb.getHeight();
        }
        double pps = Math.min(pxpsx, pypsy);

        // calc offset: based on center point staying in center
        double wx0 = bb.getX() + bb.getWidth() / 2;  // world midpoint
        double wy0 = bb.getY() + bb.getHeight() / 2;
        double x0  = displayX + displayWidth / 2 - pps * wx0;
        double y0  = displayY + displayHeight / 2 + pps * wy0;

        AffineTransform cat = new AffineTransform(pps, 0.0, 0.0, -pps, x0,
                                                  y0);

        // rotate if we need to
        if (rotate) {
            cat.rotate(Math.PI / 2, wx0, wy0);
        }

        if (debug) {
            System.out.println("Navigation calcTransform = " + displayX + " "
                               + displayY + " " + displayWidth + " "
                               + displayHeight);
            System.out.println("  world = " + bb);
            System.out.println("  scale/origin = " + pps + " " + x0 + " "
                               + y0);
            System.out.println("  transform = " + cat);
        }
        return cat;
    }

    /**
     * Get current MapArea .
     * @param   rect   place results here, or null to create new Object
     * @return  rect
     */
    public ProjectionRect getMapArea(ProjectionRect rect) {
        if (rect == null) {
            rect = new ProjectionRect();
        }

        double width  = pwidth / pix_per_world;
        double height = pheight / pix_per_world;

        // center point
        double wx0 = (pwidth / 2 - pix_x0) / pix_per_world;
        double wy0 = (pix_y0 - pheight / 2) / pix_per_world;

        rect.setRect(wx0 - width / 2, wy0 - height / 2,  // minx, miny
                     width, height);                     // width, height

        return rect;
    }

    /**
     * Set the map area
     *
     * @param ma  new map area
     */
    public void setMapArea(Rectangle2D ma) {
        if (debugMapArea) {
            System.out.println("Navigation setMapArea " + ma);
        }

        bb.setRect(ma);
        zoom.push();

        mapAreaIsSet = true;
        if (screenSizeIsSet) {
            recalcFromBoundingBox();
            fireMapAreaEvent();
        }
    }

    // kludgy thing used to deal with cylindrical seams: package private

    /**
     * Set the x value of the world center
     *
     * @param wx_center  world center
     */
    void setWorldCenterX(double wx_center) {
        pix_x0 = pwidth / 2 - pix_per_world * wx_center;
    }

    /**
     * Convert a world coordinate to a display point
     *
     * @param w     world coordinate
     * @return display point
     */
    public Point2D worldToScreen(ProjectionPointImpl w) {
        return worldToScreen(w, new Point2D.Double());
    }


    /**
     * Convert a world coordinate to a display point
     *
     * @param w     world coordinate
     * @param p     projection
     * @return display point
     */
    public Point2D worldToScreen(ProjectionPointImpl w, Point2D p) {
        p.setLocation(pix_per_world * w.getX() + pix_x0,
                      -pix_per_world * w.getY() + pix_y0);
        return p;
    }

    /**
     * Convert a display point to a world coordinate
     *
     * @param p   display point
     * @param w   projection
     * @return world point
     */
    public ProjectionPointImpl screenToWorld(Point2D p,
                                             ProjectionPointImpl w) {
        w.setLocation((p.getX() - pix_x0) / pix_per_world,
                      (pix_y0 - p.getY()) / pix_per_world);
        return w;
    }


    /**
     * Convert a display point to a world coordinate
     *
     * @param p     display point
     * @return corresponding point in world coordinates
     */
    public ProjectionPointImpl screenToWorld(Point2D p) {
        return screenToWorld(p, new ProjectionPointImpl());
    }


    /**
     * Convert a display region to a world coordinate
     *
     * @param screenRect    display region
     * @return corresponding world coordinates
     */
    public Rectangle2D screenToWorld(Rectangle2D screenRect) {
        ProjectionPointImpl origin =
            screenToWorld(new Point2D.Double(screenRect.getX(),
                                             screenRect.getY()));
        ProjectionPointImpl extent =
            screenToWorld(
                new Point2D.Double(
                    screenRect.getX() + screenRect.getWidth(),
                    screenRect.getY() + screenRect.getHeight()));

        return new Rectangle2D.Double(origin.getX(), origin.getY(),
                                      extent.getX() - origin.getX(),
                                      extent.getY() - origin.getY());
    }

    /**
     * Convert a world coordinate to screen
     *
     * @param screenRect    world region
     * @return corresponding screen coordinates
     */
    public Rectangle2D worldToScreen(Rectangle2D screenRect) {
        Point2D ul = worldToScreen(new ProjectionPointImpl(screenRect.getX(),
                         screenRect.getY()));
        Point2D lr = worldToScreen(new ProjectionPointImpl(screenRect.getX()
                         + screenRect.getWidth(), screenRect.getY()
                                                  + screenRect.getHeight()));

        return new Rectangle2D.Double(ul.getX(), lr.getY(),
                                      lr.getX() - ul.getX(),
                                      ul.getY() - lr.getY());
    }




    //domain changing calls

    /**
     * Call this to change the center of the screen's world coordinates.
     *
     * @param deltax   display coordinate x shift
     * @param deltay   display coordinate y shift
     */
    public void pan(double deltax, double deltay) {
        zoom.push();

        pix_x0 -= deltax;
        pix_y0 -= deltay;
        fireMapAreaEvent();
    }

    /**
     * Call this to zoom into a subset of the screen.
     * startx, starty are the upper left corner of the box in display coords.
     *
     * @param startx     upper left x box coord
     * @param starty     upper left y box coord
     * @param width      width of box
     * @param height     size of box
     */
    public void zoom(double startx, double starty, double width,
                     double height) {
        if (debugZoom) {
            System.out.println("zoom " + startx + " " + starty + " " + width
                               + " " + height + " ");
        }

        if ((width < 5) || (height < 5)) {
            return;
        }
        zoom.push();

        pix_x0 -= startx + width / 2 - pwidth / 2;
        pix_y0 -= starty + height / 2 - pheight / 2;
        zoom((double) pwidth / width);
    }

    /**
     * Zoom in to a point. Ignores x, y; calls {@link #zoomIn()}
     *
     * @param x   x coordinate of point
     * @param y   y coordinate of point
     */
    public void zoomIn(double x, double y) {
        zoomIn();
    }

    /**
     * Zoom out from a point. Ignores x, y; calls {@link #zoomIn()}
     *
     * @param x   x coordinate of point
     * @param y   y coordinate of point
     */
    public void zoomOut(double x, double y) {
        zoomOut();
    }

    /**
     * Zoom in default amount.
     */
    public void zoomIn() {
        //       zoom(2.0);
        zoom(1.1);
    }

    /**
     * Zoom out default amount.
     */
    public void zoomOut() {
        //        zoom(.5);
        zoom(.9);
    }

    /**
     * Zoom to a default scale.
     *
     * @param scale to zoom
     */
    public void zoom(double scale) {
        if ( !screenSizeIsSet) {
            initialScale = scale;
            return;
        }

        zoom.push();

        // change scale, but leave center point fixed
        // get these equations by solving for pix_x0, pix_y0
        // that leaves center point invariant
        double fac = (1 - scale);
        pix_x0        = scale * pix_x0 + fac * pwidth / 2;
        pix_y0        = scale * pix_y0 + fac * pheight / 2;
        pix_per_world *= scale;

        //      System.err.println ("zoom: " + scale + " ppw:" + pix_per_world);
        fireMapAreaEvent();
    }

    /**
     * Move down
     */
    public void moveDown() {
        moveDown(2);
    }

    /**
     * Move down
     *
     * @param factor how much to move
     */
    public void moveDown(double factor) {
        zoom.push();
        pix_y0 -= pheight / factor;
        fireMapAreaEvent();
    }

    /**
     * Move up.
     */
    public void moveUp() {
        moveUp(2);
    }

    /**
     * Move up.
     *
     * @param factor how much to move
     */
    public void moveUp(double factor) {
        zoom.push();
        pix_y0 += pheight / factor;
        fireMapAreaEvent();
    }

    /**
     * Move right.
     */
    public void moveRight() {
        moveRight(2);
    }

    /**
     * Move right.
     *
     * @param factor how much to move
     */
    public void moveRight(double factor) {
        zoom.push();
        pix_x0 -= pwidth / factor;
        fireMapAreaEvent();
    }

    /**
     * Move left.
     */
    public void moveLeft() {
        moveLeft(2);
    }

    /**
     * Move left.
     *
     * @param factor how much to move
     */
    public void moveLeft(double factor) {
        zoom.push();

        pix_x0 += pwidth / factor;
        fireMapAreaEvent();
    }

    /**
     * Zoom to the previous zoom.
     */
    public void zoomPrevious() {
        zoom.pop();
        fireMapAreaEvent();
    }

    /////////////////////////////////////////////////////////////////
    // private methods


    /**
     * Calculate scale and offset based on the current screen size and
     * bounding box adjust bounding box to fit inside the screen size
     */
    private void recalcFromBoundingBox() {
        if (debugRecalc) {
            System.out.println("Navigation recalcFromBoundingBox= " + bb);
            System.out.println("  " + pwidth + " " + pheight);
        }

        // decide which dimension is limiting
        double pixx_per_wx = (bb.getWidth() == 0.0)
                             ? 1
                             : pwidth / bb.getWidth();
        double pixy_per_wy = (bb.getHeight() == 0.0)
                             ? 1
                             : pheight / bb.getHeight();
        pix_per_world = Math.min(pixx_per_wx, pixy_per_wy);
        //      System.err.println ("recalc:" + pix_per_world);

        // calc the center point
        double wx0 = bb.getX() + bb.getWidth() / 2;
        double wy0 = bb.getY() + bb.getHeight() / 2;

        // calc offset based on center point
        pix_x0 = pwidth / 2 - pix_per_world * wx0;
        pix_y0 = pheight / 2 + pix_per_world * wy0;

        if (debugRecalc) {
            System.out.println("Navigation recalcFromBoundingBox done= "
                               + pix_per_world + " " + pix_x0 + " " + pix_y0);
            System.out.println("  " + pwidth + " " + pheight + " " + bb);
        }
    }


    /**
     * Fire a new map area event.  Called for zooms, pans, etc.
     */
    private synchronized void fireMapAreaEvent() {
        if (debugZoom) {
            System.out.println("newArea ");
        }

        // send out event to all listeners
        lm.sendEvent(new NewMapAreaEvent(this));
    }

    // keep stack of previous zooms
    // this should propably be made into a circular buffer

    /**
     * Class ZoomStack
     *
     * @author Unidata development team
     */
    private class ZoomStack extends java.util.ArrayList {

        /** current index */
        private int current = -1;

        /**
         * Create a ZoomStack.
         */
        ZoomStack() {
            super(20);  // stack size
        }

        /**
         * Push a new zoom onto the stack.
         */
        void push() {
            current++;
            add(current, new Zoom(pix_per_world, pix_x0, pix_y0));
        }

        /**
         * Pop a new zoom on the stack.
         */
        void pop() {
            if (current < 0) {
                return;
            }
            Zoom zoom = (Zoom) get(current);
            //      System.err.println ("pop:" + pix_per_world);
            pix_per_world = zoom.pix_per_world;
            pix_x0        = zoom.pix_x0;
            pix_y0        = zoom.pix_y0;
            current--;
        }

        /**
         * Class Zoom
         *
         * @author Unidata development team
         */
        private class Zoom {

            /** pixels per world coordinate */
            double pix_per_world;

            /** starting x */
            double pix_x0;

            /** starting y */
            double pix_y0;

            /**
             * Create a new zoom
             *
             * @param p1    pixels per world coord
             * @param p2    starting x
             * @param p3    starting y
             *
             */
            Zoom(double p1, double p2, double p3) {
                pix_per_world = p1;
                pix_x0        = p2;
                pix_y0        = p3;
            }
        }

    }

}

