/*
 * $Id: NewMapAreaEvent.java,v 1.8 2005/05/13 18:33:13 jeffmc Exp $
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



import java.awt.geom.Rectangle2D;


/**
 * Used to notify listeners that there is a new world bounding box.
 * @author John Caron
 * @version $Id: NewMapAreaEvent.java,v 1.8 2005/05/13 18:33:13 jeffmc Exp $
 */
public class NewMapAreaEvent extends java.util.EventObject {

    /** zoom flag */
    private boolean isZoom = true;

    /** map area bounds */
    private Rectangle2D bounds;


    /**
     * Creat an event for new map areas
     *
     * @param source   source of the event
     *
     */
    public NewMapAreaEvent(Object source) {
        super(source);
    }

    /**
     * Create an event for new map areas
     *
     * @param source    source of event
     * @param isZoom    true if event from zoom
     * @param bounds    new map area
     *
     */
    public NewMapAreaEvent(Object source, boolean isZoom,
                           Rectangle2D bounds) {
        super(source);
        this.isZoom = isZoom;
        this.bounds = bounds;
    }


    /**
     *  Is this a zoom event (as opposed to a selected area event)
     *
     *  @return is a zoom event.
     */

    public boolean getIsZoom() {
        return isZoom;
    }


    /**
     * Get the Bounds property.
     *  @return The Bounds
     */
    public Rectangle2D getBounds() {
        return bounds;
    }


}

