/*
 * $Id: TrackControl.java,v 1.69 2007/08/21 11:32:08 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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



package ucar.unidata.idv.control.storm;


import ucar.unidata.data.storm.*;



import ucar.visad.display.*;

import java.util.List;
import java.util.ArrayList;




/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class WayDisplayState {

    /** _more_          */
    private Way way;

    /** _more_          */
    private boolean visible = true;

    /** _more_          */
    List<Displayable> displayables = new ArrayList<Displayable>();

    public WayDisplayState() {
    }


    /**
     * _more_
     *
     * @param way _more_
     */
    public WayDisplayState(Way way) {
        this.way = way;
    }


    /**
     * _more_
     *
     * @param displayable _more_
     */
    public void addDisplayable(Displayable displayable) {
        displayables.add(displayable);
    }



    /**
     * Set the Way property.
     *
     * @param value The new value for Way
     */
    public void setWay(Way value) {
        way = value;
    }

    /**
     * Get the Way property.
     *
     * @return The Way
     */
    public Way getWay() {
        return way;
    }

    /**
     * Set the Visible property.
     *
     * @param value The new value for Visible
     */
    public void setVisible(boolean value) throws Exception {
        this.visible = value;
        for(Displayable displayable: displayables) {
            displayable.setVisible(visible);
        }
    }

    /**
     * Get the Visible property.
     *
     * @return The Visible
     */
    public boolean getVisible() {
        return visible;
    }



}

