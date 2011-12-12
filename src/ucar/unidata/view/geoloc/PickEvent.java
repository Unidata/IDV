/*
 * $Id: PickEvent.java,v 1.8 2005/05/13 18:33:14 jeffmc Exp $
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



import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseEvent;


/**
 * User wants to pick an object at 2D location.
 * @author John Caron
 * @version $Id: PickEvent.java,v 1.8 2005/05/13 18:33:14 jeffmc Exp $
 */
public class PickEvent extends java.util.EventObject {

    /** pick point */
    Point2D point;

    /**
     *  The bounds property.
     */
    private Rectangle2D bounds;

    /**
     *  The mouseEvent property.
     */
    private MouseEvent mouseEvent;

    /**
     * Create a new PickEvent
     *
     * @param source      source of event
     * @param location    location of event
     * @param mouseEvent  mouse event sparking this event
     */
    public PickEvent(Object source, Point2D location, MouseEvent mouseEvent) {
        super(source);
        this.point      = location;
        this.mouseEvent = mouseEvent;
    }

    /**
     * Create a new PickEvent
     *
     * @param source      source of event
     * @param bounds      bounding rectangle (for rubber banding)
     * @param mouseEvent  mouse event sparking this event
     */
    public PickEvent(Object source, Rectangle2D bounds,
                     MouseEvent mouseEvent) {
        super(source);
        this.bounds     = bounds;
        this.mouseEvent = mouseEvent;
    }


    /**
     *  Is this pick event a region (as opposed to a point).
     *
     *  @return Is a region.
     */
    public boolean isRegionSelect() {
        return bounds != null;
    }

    /**
     *  Is this pick event a point (as opposed to a region).
     *
     *  @return Is a point.
     */
    public boolean isPointSelect() {
        return point != null;
    }


    /**
     *  Get the Bounds property.
     *
     *  @return The Bounds
     */
    public Rectangle2D getBounds() {
        return bounds;
    }



    /**
     *  Return the pick point.
     *
     *  @return The pick point.
     */
    public Point2D getLocation() {
        return point;
    }


    /**
     *  Get the MouseEvent property.
     *
     *  @return The MouseEvent
     */
    public MouseEvent getMouseEvent() {
        return mouseEvent;
    }


}
