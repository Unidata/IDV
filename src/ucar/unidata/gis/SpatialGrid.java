/*
 * $Id: SpatialGrid.java,v 1.14 2005/05/13 18:29:33 jeffmc Exp $
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

package ucar.unidata.gis;



import java.awt.geom.*;


/**
 * Fast implementation for tracking data overlap and closest point in a
 * 2D region.  The region of interest is divided into non-overlapping cells.
 * Each cell may contain zero or one data objects. This allows quickly finding
 * if a data object may be drawn (markIfClear) and closest drawn object to a
 * point (findClosest).
 *
 * @author caron, with design help from russ
 * @version $Id: SpatialGrid.java,v 1.14 2005/05/13 18:29:33 jeffmc Exp $
 */

public class SpatialGrid {

    /** maximum double value */
    private static final double MAX_DOUBLE = Double.MAX_VALUE;

    /** array of grid cels */
    private GridCell[] grid[];

    /** number of columns and rows */
    private int nx, ny;

    /** x, y counts */
    private int countX, countY;

    /** grid width, grid height */
    private double gridWidth, gridHeight;

    /** x, y, offsets */
    private double offsetX, offsetY;

    /** overlap */
    private double scaleOverlap = 1.0;

    /** resulting area */
    private Rectangle2D result = new Rectangle2D.Double();

    /** debug flags */
    private boolean debug        = false,
                    debugMark    = false,
                    debugMark2   = false,
                    debugClosest = false;

    /**
     * Constructor
     * @param nx maximum number of grid columns
     * @param ny maximum number of grid rows
     */
    public SpatialGrid(int nx, int ny) {
        this.nx = nx;
        this.ny = ny;
        grid    = new GridCell[ny][];
        for (int y = 0; y < ny; y++) {
            grid[y] = new GridCell[nx];
            for (int x = 0; x < nx; x++) {
                grid[y][x] = new GridCell();
            }
        }

    }

    /**
     * Set the grid scale.
     * @param bbox bounding box  we are only interested in points inside of this
     * @param cellSize  divide the bounding box into cells of this size.
     *   maximum number of grid cells is nx x ny
     */
    public void setGrid(Rectangle2D bbox, Rectangle2D cellSize) {
        offsetX = bbox.getX();
        offsetY = bbox.getY();

        // number of grid cells
        countX = Math.min(nx,
                          (int) (bbox.getWidth()
                                 / (scaleOverlap * cellSize.getWidth())));
        countY = Math.min(ny,
                          (int) (bbox.getHeight()
                                 / (scaleOverlap * cellSize.getHeight())));


        /**
         * System.err.println ("Like to be:" + (int) (bbox.getWidth()
         *                        / (scaleOverlap * cellSize.getWidth())) + ", " +
         *                                          (int) (bbox.getHeight()
         *                                                 / (scaleOverlap * cellSize.getHeight())));
         *
         */
        gridWidth  = bbox.getWidth() / countX;
        gridHeight = bbox.getHeight() / countY;

        if (debug) {
            System.out.println("SpatialGrid size " + gridWidth + " "
                               + gridHeight + " = " + countX + " by "
                               + countY + " scaleOverlap= " + scaleOverlap);
        }
    }

    /**
     * Set how much the data may overlap.
     * @param overlap   percent overlap
     */
    public void setOverlap(int overlap) {
        // overlap limited to [0, 50%]
        double dover = Math.max(0.0, Math.min(.01 * overlap, .50));
        scaleOverlap = 1.0 - dover;
    }

    /** print the grid */
    public void print() {
        for (int y = 0; y < countY; y++) {
            for (int x = 0; x < countX; x++) {
                if (grid[y][x].clear) {
                    System.out.print("0");
                } else {
                    System.out.print("1");
                }
            }
            System.out.println("");
        }
    }


    /** clear all the grid cells */
    public void clear() {
        for (int y = 0; y < countY; y++) {
            for (int x = 0; x < countX; x++) {
                grid[y][x].clear = true;
            }
        }
    }

    /**
     * Check if the given rect intersects an already drawn one.
     * If not, set the corresponding cell as marked, store object, return true,
     * meaning "ok to draw".
     * @param rect the bounding box of the thing we want to draw
     * @param o store this object
     * @return true if inside the bounding box and no intersection
     */
    public boolean markIfClear(Rectangle2D rect, Object o) {
        double centerX = rect.getX() + rect.getWidth() / 2;
        double centerY = rect.getY() + rect.getHeight() / 2;

        int    indexX  = (int) ((centerX - offsetX) / gridWidth);
        int    indexY  = (int) ((centerY - offsetY) / gridHeight);

        if (debugMark) {
            System.out.print("markIfClear " + rect.getX() + " " + rect.getY()
                             + " " + rect.getWidth() + " " + rect.getHeight()
                             + " " + indexX + " " + indexY);
        }

        if ((indexX < 0) || (indexX >= countX) || (indexY < 0)
                || (indexY >= countY)) {  // outside box
            if (debugMark) {
                System.out.println("   NOT OK(0)");
            }
            return false;
        }

        GridCell gwant = grid[indexY][indexX];
        if ( !gwant.clear) {  // already taken
            if (debugMark) {
                System.out.println("   NOT OK(1)");
            }
            return false;
        }

        // check the surrounding points
        for (int y = Math.max(0, indexY - 1);
                y <= Math.min(countY - 1, indexY + 1); y++) {
            for (int x = Math.max(0, indexX - 1);
                    x <= Math.min(countX - 1, indexX + 1); x++) {
                GridCell gtest = grid[y][x];
                if (debugMark2) {
                    //                    System.out.println("   test " + x + " " + y + " "
                    //                                       + gtest);
                }

                if (gtest.clear) {
                    continue;
                }

                if (intersectsOverlap(rect, gtest.rect)) {  // hits an adjacent rectangle
                    if (debugMark) {
                        System.out.println("   NOT OK(2)");
                    }
                    return false;
                } else if (debugMark) {
                    System.out.println("  not intersect " + gtest.rect);
                }

            }
        }

        if (debugMark) {
            System.out.println("   OK");
        }

        // its ok to use
        gwant.clear = false;
        gwant.rect  = rect;
        gwant.o     = o;
        return true;
    }

    /**
     * Find the closest marked cell to the given point
     * @param pt   find the closest marked cell to this point
     * @return the object associated with the closest cell, or null if none
     */
    public Object findClosest(Point2D pt) {
        Object o      = null;
        int    indexX = (int) ((pt.getX() - offsetX) / gridWidth);
        int    indexY = (int) ((pt.getY() - offsetY) / gridHeight);

        if (debugClosest) {
            System.out.println("findClosest " + pt + " " + indexX + " "
                               + indexY);
        }

        if ((indexX < 0) || (indexX >= countX) || (indexY < 0)
                || (indexY >= countY)) {  // outside box
            return null;
        }

        GridCell gwant = grid[indexY][indexX];
        if ( !gwant.clear) {  // that was easy
            return gwant.o;
        }

        // check the surrounding points along perimeter of increasing diameter
        for (int p = 1; p < Math.max(countX - 1, countY - 1); p++) {
            if (null != (o = findClosestAlongPerimeter(pt, indexX, indexY,
                                                       p))) {
                return o;
            }
        }

        return null;  // nothing found
    }

    /**
     * search for closest marked cell along the perimeter of square of cells
     * with center cell[x,y] and side of length 2*perimeter+1
     * Find the closest point along the perimeter
     *
     * @param pt            point to check
     * @param centerX       center x position
     * @param centerY       center y position
     * @param perimeter     perimeter definition
     * @return closest marked object
     */
    private Object findClosestAlongPerimeter(Point2D pt, int centerX,
                                             int centerY, int perimeter) {
        Object closestO = null;
        double closestD = MAX_DOUBLE;

        // top and bottom row
        for (int y = centerY - perimeter; y <= centerY + perimeter;
                y += 2 * perimeter) {
            for (int x = centerX - perimeter; x <= centerX + perimeter; x++) {
                double distance = distanceSq(pt, x, y);
                if (distance < closestD) {
                    closestO = grid[y][x].o;
                    closestD = distance;
                    if (debugClosest) {
                        System.out.println("   closest " + grid[y][x]);
                    }
                }
            }
        }

        // middle rows
        for (int y = centerY - perimeter + 1; y <= centerY + perimeter - 1;
                y++) {
            for (int x = centerX - perimeter; x <= centerX + perimeter;
                    x += 2 * perimeter) {
                double distance = distanceSq(pt, x, y);
                if (distance < closestD) {
                    closestO = grid[y][x].o;
                    closestD = distance;
                    if (debugClosest) {
                        System.out.println("   closest " + grid[y][x]);
                    }
                }
            }
        }

        return closestO;
    }


    /**
     * Return distance**2 from pt to center of marked cell[x,y]
     * if out of bbox or cell not marked, return MAX_DOUBLE
     *
     * @param pt             point
     * @param indexX         x index
     * @param indexY         y index
     * @return distance squared from point to center of marked cell
     */
    private double distanceSq(Point2D pt, int indexX, int indexY) {
        if ((indexX < 0) || (indexX >= countX) || (indexY < 0)
                || (indexY >= countY)) {  // outside bounding box
            return MAX_DOUBLE;
        }

        GridCell gtest = grid[indexY][indexX];
        if (gtest.clear) {  // nothing in this cell
            return MAX_DOUBLE;
        }

        // get distance from center of cell
        Rectangle2D rect = gtest.rect;
        double      dx   = rect.getX() + rect.getWidth() / 2 - pt.getX();
        double      dy   = rect.getY() + rect.getHeight() / 2 - pt.getY();
        return (dx * dx + dy * dy);
    }

    /**
     * See if there is overlap between the intersects
     *
     * @param r1         first rectangle
     * @param r2         second rectangle
     * @return true if there is overlap
     */
    private boolean intersectsOverlap(Rectangle2D r1, Rectangle2D r2) {
        if (scaleOverlap >= 1.0) {
            return r1.intersects(r2);
        }

        Rectangle2D.intersect(r1, r2, result);
        double area = result.getWidth() * result.getHeight();
        return (area > 0)
               && (area
                   > (1.0 - scaleOverlap) * r1.getWidth() * r1.getHeight());
    }

    /**
     * Class GridCell
     */
    private static class GridCell {

        /** clear flage */
        boolean clear = true;

        /** bounds */
        Rectangle2D rect = null;

        /** object in cell */
        Object o = null;
    }
}

/* Change History:
   $Log: SpatialGrid.java,v $
   Revision 1.14  2005/05/13 18:29:33  jeffmc
   Clean up the odd copyright symbols

   Revision 1.13  2005/03/10 18:38:29  jeffmc
   jindent and javadoc

   Revision 1.12  2004/08/23 18:21:41  dmurray
   missed some jdoc fixes

   Revision 1.11  2004/08/23 17:27:08  dmurray
   javadoc fixes

   Revision 1.10  2004/07/07 18:15:40  jeffmc
   Comment out some debug stmts

   Revision 1.9  2004/07/07 15:35:14  jeffmc
   Modify some of the debug code

   Revision 1.8  2004/02/27 21:21:53  jeffmc
   Lots of javadoc warning fixes

   Revision 1.7  2004/01/29 17:35:22  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.6  2003/06/04 18:35:03  jeffmc
   Change how the station location map does station selection.
   Now, a single click will unselect all selected stations and select the clicked station (though,
   if the clicked station was previously selected it will unselect the station).
   If the control key is down then the clicked station will be added to the set of selected stations
   (ala, windows single select/multi select behavior)

   If a drag event occurs with the shift or control key down this is treated as a select within region
   event. If the shift key is down than we unselect any stations that were selected. If the control
   key is down then we add the newly selected stations to the list of selected stations

   Revision 1.5  2000/08/18 04:15:25  russ
   Licensed under GNU LGPL.

   Revision 1.4  1999/12/20 16:26:46  dmurray
   set debug to false

   Revision 1.3  1999/12/16 22:57:32  caron
   gridded data viewer checkin

*/







