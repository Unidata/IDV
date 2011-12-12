/*
 * $Id: NewProjectionEvent.java,v 1.11 2005/05/13 18:33:14 jeffmc Exp $
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



import ucar.unidata.geoloc.ProjectionImpl;


/**
 * Used to notify listeners that there is a new Projection.
 * @author John Caron
 * @version $Id: NewProjectionEvent.java,v 1.11 2005/05/13 18:33:14 jeffmc Exp $
 */
public class NewProjectionEvent extends java.util.EventObject {

    /** projection */
    private ProjectionImpl project;

    /**
     * Create an event for a new projection
     *
     * @param source  source of event
     * @param proj    new projection
     *
     */
    public NewProjectionEvent(Object source, ProjectionImpl proj) {
        super(source);
        this.project = proj;
    }

    /**
     * Get the new projection this event is declaring.
     * @return new projection
     */
    public ProjectionImpl getProjection() {
        return project;
    }

}

